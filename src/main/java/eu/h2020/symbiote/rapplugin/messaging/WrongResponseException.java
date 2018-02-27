package eu.h2020.symbiote.rapplugin.messaging;

import lombok.Getter;

public class WrongResponseException extends RuntimeException {
    private static final long serialVersionUID = 3072896250414289567L;

    @Getter
    private RapPluginErrorResponse response;

    public WrongResponseException(int responseCode, String message) {
        super(message);
        response = new RapPluginErrorResponse(responseCode, message);
    }
}
