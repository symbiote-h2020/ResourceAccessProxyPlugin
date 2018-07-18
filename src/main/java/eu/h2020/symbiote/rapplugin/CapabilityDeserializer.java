/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.model.cim.Capability;
import eu.h2020.symbiote.rapplugin.util.Utils;
import eu.h2020.symbiote.rapplugin.value.Value;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.HttpClient;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class CapabilityDeserializer {

    private final DeserializerRegistry deserializerRegistry;
    private final String interworkingInterfaceUrl;
    private static HttpClient httpClient;
    
    public static void setHttpClient(HttpClient httpClient) {
        CapabilityDeserializer.httpClient = httpClient;
    }

    public static Map<String, Map<String, Value>> deserialize(String interworkingInterfaceUrl, DeserializerRegistry deserializerRegistry, String internalId, String capabilitiesJson) {
        return new CapabilityDeserializer(interworkingInterfaceUrl, deserializerRegistry).deserializeInternal(internalId, capabilitiesJson);
    }
    
    public static Map<String, Map<String, Value>> deserialize(String interworkingInterfaceUrl, String internalId, String capabilitiesJson) {
        return new CapabilityDeserializer(interworkingInterfaceUrl, null).deserializeInternal(internalId, capabilitiesJson);
    }
    
    private CapabilityDeserializer(String interworkingInterfaceUrl, DeserializerRegistry deserializerRegistry) {
        this.interworkingInterfaceUrl = interworkingInterfaceUrl;
        this.deserializerRegistry = deserializerRegistry == null ? new DeserializerRegistry() : deserializerRegistry;
    }

    private Map<String, Map<String, Value>> deserializeInternal(String internalId, String capabilitiesJson) {
        Map<String, Map<String, Value>> result = new HashMap<>();
        try {
            List<Capability> capabilitiesDefined = Utils.getActuatorCapabilitiesParameterDefinition(httpClient, interworkingInterfaceUrl, internalId);
            Map<String, String> capabilitiesPresent = extractCapabilities(capabilitiesJson);
            for (Map.Entry<String, String> capabilityPresent : capabilitiesPresent.entrySet()) {
                Optional<Capability> temp = capabilitiesDefined.stream().filter(x -> x.getName().equals(capabilityPresent.getKey())).findAny();
                if (!temp.isPresent()) {
                    throw new RuntimeException("provided capability '" + capabilityPresent.getKey() + "' is not defined");
                }
                Capability capabilityDefined = temp.get();
                ParameterDeserializer.setHttpClient(httpClient);
                result.put(
                        capabilityPresent.getKey(),                        
                        ParameterDeserializer.deserialize(
                                interworkingInterfaceUrl,
                                deserializerRegistry,
                                capabilityDefined.getParameters(),
                                internalId,
                                capabilityPresent.getValue()));
            }
            return result;
        } catch (IOException ex) {
            Logger.getLogger(CapabilityDeserializer.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private Map<String, String> extractCapabilities(String capabilitiesJson) throws IOException {
        Map<String, String> result = new HashMap<>();
        JsonParser parser = new ObjectMapper().getFactory().createParser(capabilitiesJson);
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
