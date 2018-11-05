package eu.h2020.symbiote.rapplugin.value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ComplexValue implements Value<Map<String, Value>> {

    private final Map<String, Value> values;

    public ComplexValue() {
        this.values = new HashMap<>();
    }

    public ComplexValue(Map<String, Value> values) {
        this.values = values;
    }

    @Override
    public Map<String, Value> get() {
        return getValues();
    }

    public void addValue(String name, Value value) {
        getValues().put(name, value);
    }

    public Map<String, Value> getValues() {
        return values;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + Objects.hashCode(this.values);
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
        final ComplexValue other = (ComplexValue) obj;
        if (!Objects.equals(this.values, other.values)) {
            return false;
        }
        return true;
    }

    @Override
    public JsonNode asJson() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        values.forEach((k, v) -> result.set(k, v.asJson()));
        return result;
    }

    public <T> T asCustom(Class<T> clazz) {
        try {
            return new ObjectMapper().treeToValue(asJson(), clazz);
        } catch (JsonProcessingException ex) {
            throw new ValueCastException("could not cast value as custom class '" + clazz.getSimpleName() + "'");
        }
    }

}
