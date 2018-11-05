/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.value;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ValueCastException extends RuntimeException {

    public ValueCastException(String message, Throwable cause) {
        super(message, cause);
    }

    public ValueCastException(String message) {
        super(message);
    }
}
