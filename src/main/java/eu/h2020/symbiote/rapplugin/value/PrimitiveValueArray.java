package eu.h2020.symbiote.rapplugin.value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class PrimitiveValueArray<T> implements Value<List<T>> {

    private final List<T> values;

    public static <T> PrimitiveValueArray<T> create(T... values) {
        return new PrimitiveValueArray<>(values);
    }

    public PrimitiveValueArray(T... values) {
        this.values = Arrays.asList(values);
    }

    @Override
    public boolean isPrimitiveArray() {
        return true;
    }

    @Override
    public JsonNode asJson() {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        values.forEach((value) -> {
            result.add(new ObjectMapper().convertValue(value, JsonNode.class));
        });
        return result;
    }

    @Override
    public List<T> get() {
        return values;
    }

    public <T> T asCustomType(Class<T> clazz) {
        try {
            return new ObjectMapper().treeToValue(asJson(), clazz);
        } catch (JsonProcessingException ex) {
            throw new ValueCastException("could not cast value as custom class '" + clazz.getSimpleName() + "'");
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.values);
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
        final PrimitiveValueArray<?> other = (PrimitiveValueArray<?>) obj;
        if (!Objects.equals(this.values, other.values)) {
            return false;
        }
        return true;
    }

}
