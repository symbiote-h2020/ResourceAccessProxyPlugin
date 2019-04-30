/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.rapplugin.value.Value;
import eu.h2020.symbiote.rapplugin.value.ValueDeserializer;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ParameterDeserializer {

    private ValueDeserializer valueDeserializer;
    private ObjectMapper mapper;

    public ParameterDeserializer() {
        valueDeserializer = new ValueDeserializer();
        mapper = new ObjectMapper();
    }

    public Map<String, Value> deserialize(String parametersJson) throws ValidationException {
        Map<String, Value> result = new HashMap<>();
        try {
            Map<String, String> parametersPresent = extractParameters(parametersJson);
            for (Map.Entry<String, String> parameterPresent : parametersPresent.entrySet()) {

                Value deserializedValue = null;
                JsonParser jsonParser = mapper.getFactory().createParser(parameterPresent.getValue());
                DeserializationContext deserializationContext = mapper.getDeserializationContext();
                deserializedValue = valueDeserializer.deserialize(jsonParser, deserializationContext);
                result.put(parameterPresent.getKey(), deserializedValue);
            }
            return result;
        } catch (IOException ex) {
            throw new RuntimeException("could not deserialize parameter", ex);
        }
    }

    private Map<String, String> extractParameters(String parametersJson) throws IOException, ValidationException {
        Map<String, String> result = new HashMap<>();
        JsonParser parser = new ObjectMapper().getFactory().createParser(parametersJson);
        JsonNode node = parser.readValueAsTree();
        if (!node.isArray()) {
            throw new ValidationException("provided JSON is not an array");
        }
        Iterator<JsonNode> parameterIterator = node.elements();
        while (parameterIterator.hasNext()) {
            JsonNode parameter = parameterIterator.next();
            if (!parameter.isObject()) {
                throw new ValidationException("each parameter must be contained in a seperate JSON object");
            }
            parameter.fields().forEachRemaining(x -> result.put(x.getKey(), x.getValue().toString()));
        }
        return result;
    }

}
