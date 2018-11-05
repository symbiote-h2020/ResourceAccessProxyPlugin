/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.value;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class ComplexValueArray implements Value<List<? extends Value>> {

    List<ComplexValue> values;

    public ComplexValueArray() {
        this.values = new ArrayList<>();
    }

    public ComplexValueArray(List<ComplexValue> values) {
        this.values = values;
    }

    @Override
    public JsonNode asJson() {
        ArrayNode result = JsonNodeFactory.instance.arrayNode();
        values.forEach(x -> result.add(x.asJson()));
        return result;
    }

    public <T> List<T> asCustom(Class<T> clazz) {
        return values.stream().map(x -> x.asCustom(clazz)).collect(Collectors.toList());
    }

    @Override
    public boolean isComplexArray() {
        return true;
    }

    @Override
    public List<? extends Value> get() {
        return values;
    }
}
