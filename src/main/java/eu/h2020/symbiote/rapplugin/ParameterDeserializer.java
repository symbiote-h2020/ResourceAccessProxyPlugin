/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.rapplugin.util.Utils;
import eu.h2020.symbiote.rapplugin.value.CustomTypeValue;
import eu.h2020.symbiote.rapplugin.value.Value;
import eu.h2020.symbiote.rapplugin.value.ValueDeserializer;
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
public class ParameterDeserializer {

    private final DeserializerRegistry deserializerRegistry;
    private final String registrationHandlerUrl;
    private final List<Parameter> parameterDefinition;
    private static HttpClient httpClient;

    public static void setHttpClient(HttpClient httpClient) {
        ParameterDeserializer.httpClient = httpClient;
    }

    private ParameterDeserializer(String registrationHandlerUrl, DeserializerRegistry deserializerRegistry, List<Parameter> parameterDefinition) {
        this.registrationHandlerUrl = registrationHandlerUrl;
        this.deserializerRegistry = deserializerRegistry == null ? new DeserializerRegistry() : deserializerRegistry;
        this.parameterDefinition = parameterDefinition;
    }

    public static Map<String, Value> deserialize(String registrationHandlerUrl, String internalId, String parametersJson) {
        return new ParameterDeserializer(registrationHandlerUrl, null, null).deserializeInternal(internalId, parametersJson);
    }

    public static Map<String, Value> deserialize(String registrationHandlerUrl, DeserializerRegistry deserializerRegistry, String internalId, String parametersJson) {
        return new ParameterDeserializer(registrationHandlerUrl, deserializerRegistry, null).deserializeInternal(internalId, parametersJson);
    }

    public static Map<String, Value> deserialize(String registrationHandlerUrl, DeserializerRegistry deserializerRegistry, List<Parameter> parameterDefinition, String internalId, String parametersJson) {
        return new ParameterDeserializer(registrationHandlerUrl, deserializerRegistry, parameterDefinition).deserializeInternal(internalId, parametersJson);
    }

    private Map<String, Value> deserializeInternal(String internalId, String parametersJson) {
        Map<String, Value> result = new HashMap<>();
        try {
            List<Parameter> parametersDefined = parameterDefinition == null
                    ? Utils.getServiceParameterDefinition(httpClient, registrationHandlerUrl, internalId)
                    : parameterDefinition;
            Map<String, String> parametersPresent = extractParameters(parametersJson);
            Optional<Parameter> mandatoryParameterNotPresent = parametersDefined.stream()
                    .filter(x -> x.isMandatory())
                    .filter(x -> !parametersPresent.containsKey(x.getName()))
                    .findAny();
            if (mandatoryParameterNotPresent.isPresent()) {
                throw new RuntimeException("mandatory parameter '" + mandatoryParameterNotPresent.get().getName() + "' not present");
            }

            for (Map.Entry<String, String> parameterPresent : parametersPresent.entrySet()) {
                Optional<Parameter> temp = parametersDefined.stream().filter(x -> x.getName().equals(parameterPresent.getKey())).findAny();
                if (!temp.isPresent()) {
                    throw new RuntimeException("provided parameter '" + parameterPresent.getKey() + "' is not defined");
                }
                Parameter parameterDefined = temp.get();
                Value deserializedValue = null;
                ObjectMapper mapper = new ObjectMapper();
                JsonParser jsonParser = mapper.getFactory().createParser(parameterPresent.getValue());
                DeserializationContext deserializationContext = mapper.getDeserializationContext();
                deserializedValue = new ValueDeserializer(parameterDefined.getDatatype(), deserializerRegistry)
                        .deserialize(jsonParser, deserializationContext);
                // check restrictions here?
                if (!RestrictionChecker.checkRestrictions(deserializedValue, parameterDefined.getRestrictions())) {
                    throw new RuntimeException("restrictions violated for parameter '" + parameterPresent.getKey() + "'");
                }
                result.put(parameterPresent.getKey(), deserializedValue);
            }
            return result;
        } catch (IOException ex) {
            throw new RuntimeException("Can not deserialize because I can not access " + registrationHandlerUrl, ex);
        }
    }

    private Map<String, String> extractParameters(String parametersJson) throws IOException {
        Map<String, String> result = new HashMap<>();
        JsonParser parser = new ObjectMapper().getFactory().createParser(parametersJson);
        JsonNode node = parser.readValueAsTree();
        if (!node.isArray()) {
            throw new RuntimeException("provided JSON is not an array");
        }
        node.elements().forEachRemaining(parameter -> {
            if (!parameter.isObject()) {
                throw new RuntimeException("each parameter must be contained in a seperate JSON object");
            }
            parameter.fields().forEachRemaining(x -> result.put(x.getKey(), x.getValue().toString()));
        });
        return result;
    }

}
