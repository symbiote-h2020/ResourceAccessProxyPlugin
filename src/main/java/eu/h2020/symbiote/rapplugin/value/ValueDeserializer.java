/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.value;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import eu.h2020.symbiote.model.cim.ComplexDatatype;
import eu.h2020.symbiote.model.cim.ComplexProperty;
import eu.h2020.symbiote.model.cim.DataProperty;
import eu.h2020.symbiote.model.cim.Datatype;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.PrimitiveProperty;
import eu.h2020.symbiote.rapplugin.util.Utils;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.shared.PrefixMapping;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ValueDeserializer extends StdDeserializer<Value> {

    private Datatype datatype;

    public ValueDeserializer(Datatype datatype) {
        this((Class<?>) null);
        this.datatype = datatype;
    }

    private ValueDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Value deserialize(JsonParser jp, DeserializationContext dc) throws JsonProcessingException, IOException {
        JsonNode root = jp.getCodec().readTree(jp);
        if (Utils.isPrimitiveDatatype(datatype)) {
            PrimitiveDatatype primitiveDatatype = (PrimitiveDatatype) datatype;
            RDFDatatype rdfDatatype = TypeMapper.getInstance().getTypeByName(
                    PrefixMapping.Extended.expandPrefix(primitiveDatatype.getBaseDatatype()));
            if (rdfDatatype == null) {
                throw new RuntimeException("unknown primitive datatype '" + primitiveDatatype.getBaseDatatype() + "'");
            }
            if (datatype.isArray()) {
                // array of primitive datatype
                if (!root.isArray()) {
                    throw new RuntimeException("parameter with isArray=true must contain array");
                }
                if (rdfDatatype.getJavaClass() == null) {
                    throw new RuntimeException("no representing java class found for datatype '" + primitiveDatatype.getBaseDatatype() + "'");
                }
                List<JsonNode> elements = Utils.toList(root.elements());
                Object array = Array.newInstance(rdfDatatype.getJavaClass(), elements.size());
                for (int i = 0; i < elements.size(); i++) {
                    Array.set(array, i, parseValue(elements.get(i), rdfDatatype));
                }
                return new PrimitiveValue(array, rdfDatatype.getURI(), true);
            } else {
                // single primitive datatype
                return new PrimitiveValue(
                        parseValue(root, rdfDatatype),
                        rdfDatatype.getURI());
            }
        } else if (Utils.isComplexDatatype(datatype)) {
            ComplexDatatype complexDatatype = (ComplexDatatype) datatype;
            ComplexValue result = new ComplexValue();
            if (datatype.isArray()) {
                if (!root.isArray()) {
                    throw new RuntimeException("parameter with isArray=true must contain array");
                }
                // array of complex datatype
            } else {
                if (!root.isObject()) {
                    throw new RuntimeException("parameter with complex datatype must contain object");
                }
                // recurse for fields of complex datatype
                Map<String, JsonNode> fields = Utils.toMap(root.fields());
                for (Map.Entry<String, JsonNode> field : fields.entrySet()) {
                    result.addValue(field.getKey(), recurse(field.getValue(), complexDatatype, field.getKey()));
                }
                return result;
            }
        }
        throw new RuntimeException();
    }

    private Datatype getSubDatatype(ComplexDatatype datatype, String field) {
        Optional<DataProperty> subDatatype = datatype.getDataProperties().stream().filter(x -> x.getName().equals(field)).findAny();
        if (!subDatatype.isPresent()) {
            throw new RuntimeException("property '" + field + "' not defined in datatype");
        }
        if (PrimitiveProperty.class.isAssignableFrom(subDatatype.get().getClass())) {
            return ((PrimitiveProperty) subDatatype.get()).getPrimitiveDatatype();
        } else if (ComplexProperty.class.isAssignableFrom(subDatatype.get().getClass())) {
            return ((ComplexProperty) subDatatype.get()).getDatatype();
        }
        throw new RuntimeException("unknown subclass of DataProperty used");
    }

    private Value recurse(JsonNode objectNode, ComplexDatatype datatype, String field) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jsonParser = mapper.getFactory().createParser(objectNode.toString());
        DeserializationContext deserializationContext = mapper.getDeserializationContext();
        Datatype temp = this.datatype;
        this.datatype = getSubDatatype(datatype, field);
        Value result = deserialize(jsonParser, deserializationContext);
        this.datatype = temp;
        return result;
    }

    private Object parseValue(JsonNode node, RDFDatatype rdfDatatype) {
        if (!node.isValueNode()) {
            throw new RuntimeException("parameter with primitive datatype must contain single value");
        }
        String value = node.isTextual() ? node.asText() : node.toString();
        try {
            return rdfDatatype.parse(value);
        } catch (DatatypeFormatException ex) {
            throw new RuntimeException("value '" + value + "' could not be parsed to datatype '" + rdfDatatype.getURI() + "'");
        }
    }
}
