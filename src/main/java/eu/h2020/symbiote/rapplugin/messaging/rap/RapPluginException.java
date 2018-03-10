package eu.h2020.symbiote.rapplugin.messaging.rap;

import eu.h2020.symbiote.rapplugin.messaging.RapPluginErrorResponse;
import lombok.Getter;

/**
 * This exception is thrown when implementation can not execute some operation
 * e.g. getting resource, actuating or invoking service.
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
public class RapPluginException extends RuntimeException {
    private static final long serialVersionUID = 3072896250414289567L;

    @Getter
    private RapPluginErrorResponse response;

    /**
     * Creates exception with message and response code.
     * 
     * @param responseCode will be returned to client
     * @param message message returned to client
     */
    public RapPluginException(int responseCode, String message) {
        super(message);
        response = new RapPluginErrorResponse(responseCode, message);
    }
}
