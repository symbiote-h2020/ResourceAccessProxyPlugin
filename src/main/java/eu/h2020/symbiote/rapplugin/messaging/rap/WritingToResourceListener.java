package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.List;
import java.util.Map;

import eu.h2020.symbiote.cloud.model.data.Result;
import eu.h2020.symbiote.rapplugin.domain.Capability;

public interface WritingToResourceListener {
    /**  
     * This method is called when DSI/RAP is received request for actuation.
     * In the implementation of this method put here a call to the platform 
     * with internal resource id and parameters for setting the actuator value.
     * 
     * @param resourceId internal resource id
     * @param parameters service/actuation parameters
     * @return service result for calling service and for actuation null if actuation is triggered
     */
    Result<Object> writeResource(String resourceId, Map<String,Capability> parameters);
}
