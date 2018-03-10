package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.Map;

import eu.h2020.symbiote.rapplugin.domain.Parameter;

public interface InvokingServiceListener {
    /**  
     * This method is called when DSI/RAP is received request for actuation.
     * In the implementation of this method put here a call to the platform 
     * with internal resource id and parameters for setting the actuator value.
     * 
     * @param resourceId internal resource id
     * @param parameters service/actuation parameters
     * @return service result for calling service and for actuation null if actuation is triggered
     */
    Object invokeService(String resourceId, Map<String,Parameter> parameters);
}
