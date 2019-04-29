package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.Map;

import eu.h2020.symbiote.rapplugin.domain.Capability;

/**
 * This listener is called when RAP sends request over RabbitMQ to execute
 * actuating some resource.
 * 
 * Listener needs to be registered in RapPlugin.
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 * 
 * @deprecated Replaced by {@link ActuatorAccessListener}
 */
@Deprecated
public interface ActuatingResourceListener {
    /**  
     * This method is called when RAP received request for actuation.
     * In the implementation of this method put here a call to the platform 
     * with internal resource id and parameters for setting the actuator value.
     * 
     * @param resourceId internal resource id
     * @param parameters actuation capabilities with parameters
     * 
     * @throws RapPluginException when actuation can not be executed
     */
    void actuateResource(String resourceId, Map<String,Capability> parameters);
}
