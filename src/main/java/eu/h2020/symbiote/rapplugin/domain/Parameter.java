package eu.h2020.symbiote.rapplugin.domain;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents parameters of capability when actuating and parameters in invoking
 * service.
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
@Data
@NoArgsConstructor
public class Parameter {
    
    public Parameter(String name, Object value) {
        this.name = name;
        this.value = value;
    }
    /**
     * The name of parameter.
     */
    private String name;
    
    /**
     * The value of parameter.
     */
    private Object value;

    /**
     * Checks if value can be serialized to JSON and back.
     * 
     * @param type of value
     * @return typed value
     */
    public <T> T getValueAsType(TypeReference<T> type) {
        ObjectMapper mapper = new ObjectMapper();
        
        String json;
        try {
            json = mapper.writeValueAsString(value);
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("Can not convert parameter value to " + type.getType().getTypeName() + ".", e);
        }
    }
}
