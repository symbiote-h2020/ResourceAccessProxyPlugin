package eu.h2020.symbiote.rapplugin.messaging.rap;

import eu.h2020.symbiote.rapplugin.value.Value;
import java.util.Map;

/**
 * This listener is called when RAP sends request over RabbitMQ to execute
 * actuating some resource.
 *
 * Listener needs to be registered in RapPlugin.
 *
 * @author Mario Kušek <mario.kusek@fer.hr>
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public interface ActuatorAccessListener {

    /**
     * This method is called when RAP received request for actuation. In the
     * implementation of this method put here a call to the platform with
     * internal resource id and parameters for setting the actuator value.
     *
     * @param internalId internal ID of the requested resource
     * @param capabilities capabilities to trigger. Map of capability name with
     * key-value pairs of parameter names and values
     *
     * @throws RapPluginException when actuation can not be executed
     */
    public void actuateResource(String internalId, Map<String, Map<String, Value>> capabilities);
}
