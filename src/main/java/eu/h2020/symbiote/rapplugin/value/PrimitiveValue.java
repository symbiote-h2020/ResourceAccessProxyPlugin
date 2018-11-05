package eu.h2020.symbiote.rapplugin.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Objects;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class PrimitiveValue<T> implements Value<T> {

    private final T value;
    private final boolean isArray;

    public static <T> PrimitiveValue<T> create(T value) {
        RDFDatatype datatype = TypeMapper.getInstance().getTypeByValue(value);
        if (datatype == null) {
            throw new RuntimeException("unable to determine RDF datatype of value '" + value + "'");
        }
        return new PrimitiveValue<>(value);
    }

    public PrimitiveValue(T value) {
        isArray = false;
        this.value = value;
    }

    public PrimitiveValue(T value, boolean isArray) {
        this.value = value;
        this.isArray = isArray;
    }

    @Override
    public JsonNode asJson() {
        if (isArray) {
            ArrayNode result = JsonNodeFactory.instance.arrayNode();
            Object[] data = ((Object[]) value);
            for (Object entry : data) {
                result.add(new ObjectMapper().convertValue(entry, JsonNode.class));
            }
            return result;
        } else {
            return new ObjectMapper().convertValue(value, JsonNode.class);
        }
    }

    @Override
    public T get() {
        return value;
    }

    public boolean isArray() {
        return isArray;
    }

    @Override
    public boolean isPrimitive() {
        return true;
    }

    // double, int, string, boolean
    public boolean isDouble() {
        try {
            asDouble();
            return true;
        } catch (ValueCastException ex) {
            return false;
        }
    }

    public boolean isInt() {
        try {
            asInt();
            return true;
        } catch (ValueCastException ex) {
            return false;
        }
    }

    public boolean isString() {
        try {
            asString();
            return true;
        } catch (ValueCastException ex) {
            return false;
        }
    }

    public boolean isBoolean() {
        try {
            asBoolean();
            return true;
        } catch (ValueCastException ex) {
            return false;
        }
    }

    public boolean isNumber() {
        try {
            asNumber();
            return true;
        } catch (ValueCastException ex) {
            return false;
        }
    }

    public double asDouble() {
        if (Double.class.isAssignableFrom(get().getClass())) {
            return (Double) value;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            throw new ValueCastException("value '" + value + "' cannot be cast to double", ex);
        }
    }

    public int asInt() {
        if (Integer.class.isAssignableFrom(get().getClass())) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            throw new ValueCastException("value '" + value + "' cannot be cast to int", ex);
        }
    }

    public String asString() {
        try {
            return get().toString();
        } catch (Exception ex) {
            throw new ValueCastException("value '" + value + "' cannot be cast to string", ex);
        }
    }

    public boolean asBoolean() {
        if (Boolean.class.isAssignableFrom(get().getClass())) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    public Number asNumber() {
        if (Number.class.isAssignableFrom(get().getClass())) {
            return (Number) value;
        }
        try {
            return NumberFormat.getInstance().parse(value.toString());
        } catch (ParseException ex) {
            throw new ValueCastException("value '" + value + "' cannot be cast to number", ex);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.value);
        hash = 67 * hash + (this.isArray ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PrimitiveValue<?> other = (PrimitiveValue<?>) obj;
        if (this.isArray != other.isArray) {
            return false;
        }
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

}
