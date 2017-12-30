package eu.h2020.symbiote.rapplugin.messaging;

import lombok.Getter;

public class WrongResponseException extends RuntimeException {
    private static final long serialVersionUID = 3072896250414289567L;

    @Getter
    private Object response;

    public WrongResponseException(Object response) {
        super();
        this.response = response;
    }

    public WrongResponseException(String message, Object response) {
        super(message);
        this.response = response;
    }
}
