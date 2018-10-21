package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import org.junit.Test;

import eu.h2020.symbiote.rapplugin.domain.Capability;
import eu.h2020.symbiote.rapplugin.domain.Parameter;
import eu.h2020.symbiote.rapplugin.value.PrimitiveValue;
import eu.h2020.symbiote.rapplugin.value.Value;

public class ActuatorAccessListenerAdapterTest {
    
    private String actualResourceId;
    private Map<String, Capability> actuatlParameters;
    
    @Test
    public void actuateResourceShouldConvertInputMessage() {
        // given
        Map<String, Map<String, Value>> capabilities = new HashMap<>();
        Map<String, Value> onOffCapability = new HashMap<>();
        onOffCapability.put("value", PrimitiveValue.create(true));
        capabilities.put("OnOffCapability", onOffCapability);
        
        Map<String, Value> rgbCapability = new HashMap<>();
        rgbCapability.put("r", PrimitiveValue.create(100));
        rgbCapability.put("g", PrimitiveValue.create(150));
        rgbCapability.put("b", PrimitiveValue.create(200));
        capabilities.put("RGBCapability", rgbCapability);
        
        ActuatorAccessListenerAdapter adapter = new ActuatorAccessListenerAdapter(new ActuatingResourceListener() {
            
            @Override
            public void actuateResource(String resourceId, Map<String, Capability> parameters) {
                actualResourceId = resourceId;
                actuatlParameters = parameters;
            }
        });
        
        // when
        adapter.actuateResource("some_internal_id", capabilities);
        
        // then
        assertThat(actualResourceId).isEqualTo("some_internal_id");
        assertThat(actuatlParameters).containsKeys("OnOffCapability", "RGBCapability");
        
        Capability actualOnOffCapability = actuatlParameters.get("OnOffCapability");
        assertThat(actualOnOffCapability.getParameters()).containsOnlyKeys("value");
        assertThat(actualOnOffCapability.getParameters().get("value").getValue()).isEqualTo(true);
        
        Capability actualRgbCapability = actuatlParameters.get("RGBCapability");
        assertThat(actualRgbCapability.getParameters()).containsOnlyKeys("r", "g", "b");
        assertThat(actualRgbCapability.getParameters().get("r").getValue()).isEqualTo(100);
        assertThat(actualRgbCapability.getParameters().get("g").getValue()).isEqualTo(150);
        assertThat(actualRgbCapability.getParameters().get("b").getValue()).isEqualTo(200);
    }
}
