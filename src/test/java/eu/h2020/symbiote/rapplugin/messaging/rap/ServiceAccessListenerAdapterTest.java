package eu.h2020.symbiote.rapplugin.messaging.rap;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.util.DoubleComparator;
import org.junit.Test;

import eu.h2020.symbiote.rapplugin.domain.Parameter;
import eu.h2020.symbiote.rapplugin.value.PrimitiveValue;
import eu.h2020.symbiote.rapplugin.value.Value;

public class ServiceAccessListenerAdapterTest {

    protected String actualResourceId;
    protected Map<String, Parameter> actualParameters;

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
        assertThat((Double)actualParameters.get("longitude").getValue()).isCloseTo(15.15, offset(5E-3));
        assertThat((Double)actualParameters.get("latitude").getValue()).isCloseTo(45.45, offset(5E-3));
        
    }
}
