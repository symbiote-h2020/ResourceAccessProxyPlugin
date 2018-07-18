/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import eu.h2020.symbiote.model.cim.ComplexDatatype;
import eu.h2020.symbiote.model.cim.Datatype;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class DeserializerRegistry {

    private final Map<String, JsonDeserializer<?>> deserializers;

    public DeserializerRegistry() {
        this.deserializers = new HashMap<>();
    }

    public void registerDeserializer(String nameModelClass, JsonDeserializer<?> deserializer) {
        deserializers.put(nameModelClass, deserializer);
    }

    public void registerDeserializer(String nameModelClass, Class<?> clazz) {
        deserializers.put(nameModelClass, new DefaultDeserializer<>(clazz));
    }

    public JsonDeserializer<?> unregisterDeserializer(String nameModelClass) {
        return deserializers.remove(nameModelClass);
    }

    public boolean hasDeserializer(String nameModelClass) {
        return deserializers.containsKey(nameModelClass);
    }

    public boolean hasDeserializer(Datatype datatype) {
        if (!ComplexDatatype.class.isAssignableFrom(datatype.getClass())) {
            return false;
        }
        return deserializers.containsKey(((ComplexDatatype) datatype).getBasedOnClass());
    }

    public JsonDeserializer<?> getDeserializer(Datatype datatype) {
        if (!ComplexDatatype.class.isAssignableFrom(datatype.getClass())) {
            return null;
        }
        return deserializers.get(((ComplexDatatype) datatype).getBasedOnClass());
    }

    public JsonDeserializer<?> getDeserializer(String nameModelClass) {
        return deserializers.get(nameModelClass);
    }

    public class DefaultDeserializer<T> extends StdDeserializer<T> {

        public DefaultDeserializer(Class<?> type) {
            super(type);
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException {
            return (T) new ObjectMapper().readValue(jp.readValueAsTree().toString(), this._valueClass);
        }

    }
}
