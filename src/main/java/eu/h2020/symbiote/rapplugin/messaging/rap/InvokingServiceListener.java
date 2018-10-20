package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.Map;

import eu.h2020.symbiote.model.cim.Parameter;

/**
 * This listener is called when RAP sends request over RabbitMQ to invoke
 * service.
 * 
 * Listener needs to be registered in RapPlugin.

 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
public interface InvokingServiceListener {
    /**  
     * This method is called when RAP received request for invoking service.
     * In the implementation of this method put here a call to the platform 
     * with internal resource id and map of parameters for invoking service.
     * 
     * @param resourceId internal resource id
     * @param parameters service parameters. Key is parameter name, value is parameter
     * @return result for calling service. This object will be serialized to JSON by 
     * using Jackson2 and delivered to requester
     * 
     * @throws RapPluginException when service can not be called
     */
    Object invokeService(String resourceId, Map<String,Parameter> parameters);
}
