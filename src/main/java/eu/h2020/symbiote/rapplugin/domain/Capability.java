package eu.h2020.symbiote.rapplugin.domain;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Capability {
    private String name;
    private Map<String, Parameter> parameters;
    
    public Capability(String name) {
        this(name, new HashMap<>());
    }
    
    public void addParameter(Parameter parameter) {
        parameters.put(parameter.getName(), parameter);
    }
}
