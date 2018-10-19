/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.value;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ComplexValueArray implements Value<List<Value>> {

    List<Value> values;
    
    public ComplexValueArray() {
        this.values = new ArrayList<>();
    }

    public ComplexValueArray(List<Value> values) {
        this.values = values;
    }
    
    @Override
    public boolean isComplexArray() {
        return true;
    }

    @Override
    public List<Value> get() {
        return values;
    }    
}
