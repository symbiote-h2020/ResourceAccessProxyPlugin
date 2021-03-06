package eu.h2020.symbiote.rapplugin.domain;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents Capability that can be actuated.
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Capability {
    /**
     * The name of capability.
     */
    private String name;
    
    /**
     * Map containing parameters.
     * Key in map is the name of parameter.
     * Value in map is <code>Parameter</code> class.
     */
    private Map<String, Parameter> parameters;
    
    
    /**
     * Constructs capability with name.
     * 
     * @param name
     */
    public Capability(String name) {
        this(name, new HashMap<>());
    }
    
    /**
     * Adds parameter to capability.
     * @param parameter
     */
    public void addParameter(Parameter parameter) {
        parameters.put(parameter.getName(), parameter);
    }
}
