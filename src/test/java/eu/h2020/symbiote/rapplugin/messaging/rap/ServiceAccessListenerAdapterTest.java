package eu.h2020.symbiote.rapplugin.messaging.rap;

import eu.h2020.symbiote.rapplugin.ParameterDeserializer;
import eu.h2020.symbiote.rapplugin.ValidationException;
import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import eu.h2020.symbiote.rapplugin.domain.Parameter;
import eu.h2020.symbiote.rapplugin.value.ComplexValue;
import eu.h2020.symbiote.rapplugin.value.PrimitiveValue;
import eu.h2020.symbiote.rapplugin.value.Value;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceAccessListenerAdapterTest {

    protected String actualResourceId;
    protected Map<String, Parameter> actualParameters;
    protected LongLat location;

    @Test
    public void invokeServiceShouldConvertInputMessage() {
        // given
        Map<String, Value> parameters = new HashMap<>();
        parameters.put("longitude", PrimitiveValue.create(15.15));
        parameters.put("latitude", PrimitiveValue.create(45.45));

        ServiceAccessListenerAdapter adapter = new ServiceAccessListenerAdapter(new InvokingServiceListener() {

            @Override
            public Object invokeService(String resourceId, Map<String, Parameter> parameters) {
                actualResourceId = resourceId;
                actualParameters = parameters;
                return null;
            }
        });

        // when
        adapter.invokeService("some_internal_id", parameters);

        // then
        assertThat(actualResourceId).isEqualTo("some_internal_id");
        assertThat(actualParameters).containsOnlyKeys("longitude", "latitude");
        assertThat((Double) actualParameters.get("longitude").getValue()).isCloseTo(15.15, offset(5E-3));
        assertThat((Double) actualParameters.get("latitude").getValue()).isCloseTo(45.45, offset(5E-3));

    }

    @Test
    public void invokeServiceShouldConvertInputMessage_TypeSafeInterface() {
        // given
        Map<String, Value> parametersLocation = new HashMap<>();
        parametersLocation.put("longitude", PrimitiveValue.create(15.15));
        parametersLocation.put("latitude", PrimitiveValue.create(45.45));
        Map<String, Value> parameters = new HashMap<>();
        parameters.put("location", new ComplexValue(parametersLocation));

        ServiceAccessListener adapter = new ServiceAccessListener() {
            @Override
            public String invokeService(String internalId, Map<String, Value> parameters) {
                location = parameters.get("location").asComplex().asCustom(LongLat.class);
                return "";
            }
        };

        // when
        adapter.invokeService("some_internal_id", parameters);

        // then
        assertThat(location)
                .isNotNull()
                .isInstanceOf(LongLat.class)
                .hasFieldOrPropertyWithValue("longitude", 15.15)
                .hasFieldOrPropertyWithValue("latitude", 45.45);
        assertThat(location)
                .isNotNull()
                .hasFieldOrPropertyWithValue("longitude", 15.15)
                .hasFieldOrPropertyWithValue("latitude", 45.45);
    }
}
