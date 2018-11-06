/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.model.cim.Capability;
import eu.h2020.symbiote.model.cim.ComplexDatatype;
import eu.h2020.symbiote.model.cim.ComplexProperty;
import eu.h2020.symbiote.model.cim.DataProperty;
import eu.h2020.symbiote.model.cim.Datatype;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.PrimitiveProperty;
import eu.h2020.symbiote.model.cim.Restriction;
import eu.h2020.symbiote.rapplugin.RestrictionChecker;
import eu.h2020.symbiote.rapplugin.ValidationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.shared.PrefixMapping;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ValidationHelper {

    public static void validateServicePayload(List<Parameter> parametersDefined, String payload) throws ValidationException {
        Map<String, String> parametersPresent = extractParameters(payload);
        Optional<Parameter> mandatoryParameterNotPresent = parametersDefined.stream()
                .filter(x -> x.isMandatory())
                .filter(x -> !parametersPresent.containsKey(x.getName()))
                .findAny();
        if (mandatoryParameterNotPresent.isPresent()) {
            throw new ValidationException("mandatory parameter '" + mandatoryParameterNotPresent.get().getName() + "' not present");
        }
        for (Map.Entry<String, String> parameterPresent : parametersPresent.entrySet()) {
            Optional<Parameter> temp = parametersDefined.stream().filter(x -> x.getName().equals(parameterPresent.getKey())).findAny();
            if (!temp.isPresent()) {
                throw new ValidationException("provided parameter '" + parameterPresent.getKey() + "' is not defined");
            }
            validateType(temp.get().getDatatype(), parameterPresent.getValue(), temp.get().getRestrictions());
        }
    }

    public static void validateActuatorPayload(List<Capability> capabilitiesDefined, String payload) throws ValidationException {
        Map<String, String> capabilitiesPresent = extractCapabilities(payload);

        for (Map.Entry<String, String> capabilityPresent : capabilitiesPresent.entrySet()) {
            Optional<Capability> temp = capabilitiesDefined.stream().filter(x -> x.getName().equals(capabilityPresent.getKey())).findAny();
            if (!temp.isPresent()) {
                throw new ValidationException("provided capability '" + capabilityPresent.getKey() + "' is not defined");
            }
            validateServicePayload(temp.get().getParameters(), capabilitiesPresent.get(temp.get().getName()));
        }
    }

    private static void validateType(Datatype datatype, String json, List<Restriction> restrictions) throws ValidationException {
        try {
            validateType(datatype, new ObjectMapper().readTree(json), restrictions);
        } catch (IOException ex) {
            throw new ValidationException("could not parse json", ex);
        }
    }

    private static void validateType(Datatype datatype, JsonNode node, List<Restriction> restrictions) throws ValidationException {
        if (datatype.isArray()) {
            if (!node.isArray()) {
                throw new ValidationException("parameter with isArray=true must contain array");
            }
            datatype.setArray(false);
            Iterator<JsonNode> elementIterator = node.elements();
            while (elementIterator.hasNext()) {
                validateType(datatype, elementIterator.next(), restrictions);
            }
            datatype.setArray(true);
        } else {
            if (Utils.isPrimitiveDatatype(datatype)) {
                if (!node.isValueNode()) {
                    throw new ValidationException("parameter with primitive datatype must contain single value");
                }
                PrimitiveDatatype primitiveDatatype = (PrimitiveDatatype) datatype;
                RDFDatatype rdfDatatype = TypeMapper.getInstance().getTypeByName(
                        PrefixMapping.Extended.expandPrefix(primitiveDatatype.getBaseDatatype()));
                if (rdfDatatype == null) {
                    throw new ValidationException("unknown primitive datatype '" + primitiveDatatype.getBaseDatatype() + "'");
                }
                if (rdfDatatype.getJavaClass() == null) {
                    throw new ValidationException("no representing java class found for datatype '" + primitiveDatatype.getBaseDatatype() + "'");
                }
                String value = node.isTextual() ? node.asText() : node.toString();
                try {
                    Object typedValue = rdfDatatype.parse(value);
                    RestrictionChecker.checkRestrictions(typedValue, rdfDatatype, restrictions);
                } catch (DatatypeFormatException ex) {
                    throw new ValidationException("value '" + value + "' could not be parsed to datatype '" + rdfDatatype.getURI() + "'");
                }
            } else if (Utils.isComplexDatatype(datatype)) {
                if (!node.isObject()) {
                    throw new ValidationException("parameter with complex datatype must contain object");
                }
                ComplexDatatype complexDatatype = (ComplexDatatype) datatype;
                Map<String, Datatype> definedFields = complexDatatype.getDataProperties().stream()
                        .collect(Collectors.toMap(x -> x.getName(), x -> getDatatype(x)));
                Map<String, JsonNode> presentFields = Utils.toMap(node.fields());
                if (!presentFields.keySet().containsAll(definedFields.keySet())) {
                    throw new ValidationException("missing defined properties: "
                            + definedFields.keySet().stream()
                                    .filter(x -> presentFields.containsKey(x))
                                    .collect(Collectors.joining(", ")));
                }
                for (Map.Entry<String, JsonNode> field : presentFields.entrySet()) {
                    if (!definedFields.keySet().contains(field.getKey())) {
                        throw new ValidationException("undefined property '" + field.getKey() + "'");
                    }
                    validateType(definedFields.get(field.getKey()), node, null);
                }
            }
        }
    }

    private static Datatype getDatatype(DataProperty property) {
        if (ComplexProperty.class.isAssignableFrom(property.getClass())) {
            return ((ComplexProperty) property).getDatatype();
        } else if (PrimitiveProperty.class.isAssignableFrom(property.getClass())) {
            return ((PrimitiveProperty) property).getPrimitiveDatatype();
        }
        throw new IllegalStateException("unkown subclass of DataProperty");
    }

    private static Map<String, String> extractParameters(String json) throws ValidationException {
        try {
            Map<String, String> result = new HashMap<>();
            JsonParser parser = new ObjectMapper().getFactory().createParser(json);
            JsonNode node = parser.readValueAsTree();
            if (!node.isArray()) {
                throw new ValidationException("provided JSON is not an array");
            }
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                JsonNode parameter = elements.next();
                if (!parameter.isObject()) {
                    throw new ValidationException("each parameter must be contained in a seperate JSON object");
                }
                parameter.fields().forEachRemaining(x -> result.put(x.getKey(), x.getValue().toString()));
            }
            return result;
        } catch (IOException ex) {
            throw new ValidationException("error extracting parameters from JSON", ex);
        }
    }

    private static Map<String, String> extractCapabilities(String capabilitiesJson) throws ValidationException {
        try {
            Map<String, String> result = new HashMap<>();
            JsonParser parser = new ObjectMapper().getFactory().createParser(capabilitiesJson);
            JsonNode node = parser.readValueAsTree();
            if (!node.isObject()) {
                throw new ValidationException("provided JSON is not an object");
            }
            node.fields().forEachRemaining(x -> {
                result.put(x.getKey(), x.getValue().toString());
            });
            return result;
        } catch (IOException ex) {
            throw new ValidationException("error extracting capabilities from JSON", ex);
        }
    }

}
