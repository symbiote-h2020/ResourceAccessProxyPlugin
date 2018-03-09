package eu.h2020.symbiote.rapplugin.domain;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {
    private String name;
    private Object value;

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
