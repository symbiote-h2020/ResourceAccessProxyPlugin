package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import eu.h2020.symbiote.rapplugin.domain.Capability;
import eu.h2020.symbiote.rapplugin.domain.Parameter;
import eu.h2020.symbiote.rapplugin.value.Value;

public class ActuatorAccessListenerAdapter implements ActuatorAccessListener {

    @SuppressWarnings("deprecation")
    private ActuatingResourceListener delegate;

    public ActuatorAccessListenerAdapter(@SuppressWarnings("deprecation") ActuatingResourceListener listener) {
        this.delegate = listener;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void actuateResource(String internalId, Map<String, Map<String, Value>> capabilities) {
        delegate.actuateResource(internalId, convertCapabilities(capabilities));
    }
    
    private Map<String,Capability> convertCapabilities(Map<String, Map<String, Value>> valueCapabilities) {
        Map<String, Capability> capabilities = new HashMap<>();
        for(Entry<String, Map<String, Value>> entry: valueCapabilities.entrySet()) {
            Capability capability = new Capability();
            capability.setName(entry.getKey());
            Map<String, Parameter> parameters = new HashMap<>();
           
            for(Entry<String, Value> parameterEntry: entry.getValue().entrySet()) {
                Parameter parameter = new Parameter();
                parameter.setName(parameterEntry.getKey());
                parameter.setValue(parameterEntry.getValue().get());
                parameters.put(parameterEntry.getKey(), parameter);
            }
            capability.setParameters(parameters);
            capabilities.put(entry.getKey(), capability);
        }
        
        return capabilities;
    }
}
