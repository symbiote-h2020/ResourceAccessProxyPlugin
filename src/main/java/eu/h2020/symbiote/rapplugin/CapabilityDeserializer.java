/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.rapplugin.value.Value;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class CapabilityDeserializer {

    private ParameterDeserializer parameterDeserializer;
    private ObjectMapper objectMapper;

    public CapabilityDeserializer() {
        objectMapper = new ObjectMapper();
        parameterDeserializer = new ParameterDeserializer();
    }

    public Map<String, Map<String, Value>> deserialize(String capabilitiesJson) throws ValidationException {
        Map<String, Map<String, Value>> result = new HashMap<>();
        try {
            Map<String, String> capabilitiesPresent = extractCapabilities(capabilitiesJson);
            for (Map.Entry<String, String> capabilityPresent : capabilitiesPresent.entrySet()) {
                result.put(
                        capabilityPresent.getKey(),
                        parameterDeserializer.deserialize(capabilityPresent.getValue()));
            }
            return result;
        } catch (IOException ex) {
            throw new ValidationException("Cannot deserialize capability.", ex);
        }
    }

    private Map<String, String> extractCapabilities(String capabilitiesJson) throws IOException {
        Map<String, String> result = new HashMap<>();
        JsonParser parser = objectMapper.getFactory().createParser(capabilitiesJson);
        JsonNode node = parser.readValueAsTree();
        if (!node.isObject()) {
            throw new RuntimeException("provided JSON is not an object");
        }
        node.fields().forEachRemaining(x -> {
            result.put(x.getKey(), x.getValue().toString());
        });
        return result;
    }

}
