package eu.h2020.symbiote.rapplugin.value;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 * @param <T> actual type of the value
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

    public default boolean isPrimitiveArray() {
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
        throw new RuntimeException("Value cannot be cast to complex value array");
    }

    public default PrimitiveValueArray asPrimitiveArray() {
        if (PrimitiveValueArray.class.isAssignableFrom(this.getClass())) {
            return (PrimitiveValueArray) this;
        }
        throw new RuntimeException("Value cannot be cast to primitive value array");
    }

    public JsonNode asJson();
}
