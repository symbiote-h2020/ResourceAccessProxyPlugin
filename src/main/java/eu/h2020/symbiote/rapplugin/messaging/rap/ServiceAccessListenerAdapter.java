package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.rapplugin.domain.Parameter;
import eu.h2020.symbiote.rapplugin.value.Value;

public class ServiceAccessListenerAdapter implements ServiceAccessListener {

    @SuppressWarnings("deprecation")
    private InvokingServiceListener delegate;
    
    private ObjectMapper mapper;

    public ServiceAccessListenerAdapter(@SuppressWarnings("deprecation") InvokingServiceListener listener) {
        this.delegate = listener;
        mapper = new ObjectMapper();
    }

    @Override
    public String invokeService(String internalId, Map<String, Value> parameters) {
        Object invokeServiceResult = delegate.invokeService(internalId, convertParameters(parameters));
        try {
            return mapper.writeValueAsString(
                    invokeServiceResult);
        } catch (JsonProcessingException e) {
            throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                    "Can not convert message from InvokingServiceListener.invokeService of type " + 
                    invokeServiceResult.getClass().getCanonicalName() + " to JSON.", e);
        }
    }

    private Map<String, Parameter> convertParameters(Map<String, Value> parameters) {
        Map<String, Parameter> map = new HashMap<>();
        for(Entry<String, Value> entry: parameters.entrySet()) {
            Parameter parameter = new Parameter();
            parameter.setName(entry.getKey());
            parameter.setValue(entry.getValue().get());
            map.put(entry.getKey(), parameter);
        }
        return map;
    }

}
