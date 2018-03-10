package eu.h2020.symbiote.rapplugin.messaging.rap;

import eu.h2020.symbiote.rapplugin.messaging.RapPluginErrorResponse;
import lombok.Getter;

public class RapPluginException extends RuntimeException {
    private static final long serialVersionUID = 3072896250414289567L;

    @Getter
    private RapPluginErrorResponse response;

    public RapPluginException(int responseCode, String message) {
        super(message);
        response = new RapPluginErrorResponse(responseCode, message);
    }
}
