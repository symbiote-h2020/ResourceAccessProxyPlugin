package eu.h2020.symbiote.rapplugin.value;

import java.util.Objects;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class PrimitiveValue<T> implements Value<T> {

    private final T value;
    private final String datatype;
    private final boolean isArray;

    public static <T> PrimitiveValue<T> create(T value) {
        RDFDatatype datatype = TypeMapper.getInstance().getTypeByValue(value);
        if (datatype == null) {
            throw new RuntimeException("unable to determine RDF datatype of value '" + value + "'");
        }
        return new PrimitiveValue<T>(value, datatype.getURI());
    }

    public PrimitiveValue(T value, String datatype) {
        isArray = false;
        this.value = value;
        this.datatype = datatype;
    }

    public PrimitiveValue(T value, String datatype, boolean isArray) {
        this.value = value;
        this.datatype = datatype;
        this.isArray = isArray;
    }

    @Override
    public T get() {
        return value;
    }

    public String getDatatype() {
        return datatype;
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
        return Double.class.isAssignableFrom(get().getClass());
    }

    public boolean isInt() {
        return Integer.class.isAssignableFrom(get().getClass());
    }

    public boolean isString() {
        return String.class.isAssignableFrom(get().getClass());
    }

    public boolean isBoolean() {
        return Boolean.class.isAssignableFrom(get().getClass());
    }

    public boolean isNumber() {
        return Number.class.isAssignableFrom(get().getClass());
    }

    public double asDouble() {
        return (Double) get();
    }

    public int asInt() {
        return (Integer) get();
    }

    public String asString() {
        return (String) get();
    }

    public boolean asBoolean() {
        return (Boolean) get();
    }

    public Number asNumber() {
        return (Number) get();
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.value);
        hash = 67 * hash + Objects.hashCode(this.datatype);
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
        if (!Objects.equals(this.datatype, other.datatype)) {
            return false;
        }
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }

}
