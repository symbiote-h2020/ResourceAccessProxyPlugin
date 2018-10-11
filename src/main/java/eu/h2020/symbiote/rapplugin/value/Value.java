package eu.h2020.symbiote.rapplugin.value;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public interface Value<T> {

    public T get();

    public default boolean isPrimitive() {
        return false;
    }

    public default boolean isComplex() {
        return false;
    }

    public default boolean isCustomType() {
        return false;
    }

    public default boolean isComplexArray() {
        return false;
    }

    public default PrimitiveValue<T> asPrimitive() {
        if (PrimitiveValue.class.isAssignableFrom(this.getClass())) {
            return (PrimitiveValue<T>) this;
        }
        throw new RuntimeException("Value cannot be cast to primitive value");
    }

    public default ComplexValue asComplex() {
        if (ComplexValue.class.isAssignableFrom(this.getClass())) {
            return (ComplexValue) this;
        }
        throw new RuntimeException("Value cannot be cast to complex value");
    }

    public default ComplexValueArray asComplexArray() {
        if (ComplexValueArray.class.isAssignableFrom(this.getClass())) {
            return (ComplexValueArray) this; 
        }
        throw new RuntimeException("Value cannot be cast to complex value");
    }

    public default CustomTypeValue<T> asCustomType() {
        if (CustomTypeValue.class.isAssignableFrom(this.getClass())) {
            return (CustomTypeValue<T>) this;
        }
        throw new RuntimeException("Value cannot be cast to custom type value");
    }
}
