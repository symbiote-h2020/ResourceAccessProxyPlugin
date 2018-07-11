package eu.h2020.symbiote.rapplugin.messaging.rap;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;


/**
 * This listener is called when RAP sends request over RabbitMQ to invoke
 * service.
 *
 * Listener needs to be registered in RapPlugin.
 *
 * @author Mario Kušek <mario.kusek@fer.hr>
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 *
 */
public interface ServiceAccessListener {

    /**
     * This method is called when RAP received request for invoking service. In
     * the implementation of this method put here a call to the platform with
     * internal resource id and map of parameters for invoking service.
     *
     * @param internalId internal ID of requested resource
     * @param parameters service parameters. Key is parameter name, value is
     * value
     * @return JSON object representing the result of the service call
     *
     * @throws RapPluginException when service can not be called
     */
    String invokeService(String internalId, Map<String, Object> parameters);
}
