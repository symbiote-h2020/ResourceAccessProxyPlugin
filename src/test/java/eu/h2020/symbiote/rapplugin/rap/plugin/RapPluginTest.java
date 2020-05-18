package eu.h2020.symbiote.rapplugin.rap.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.rapplugin.messaging.rap.ActuatorAccessListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.rapplugin.messaging.rap.ResourceAccessListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.ServiceAccessListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.SubscriptionListener;
import eu.h2020.symbiote.rapplugin.value.PrimitiveValue;
import eu.h2020.symbiote.rapplugin.value.Value;

/**
 * @author Mario Kušek <mario.kusek@fer.hr>
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
@RunWith(MockitoJUnitRunner.class)
public class RapPluginTest {

    private static final String RAP_PLUGIN_ID = "rap_plugin_test";

    @Mock
    private ResourceAccessListener readingAccessListener;

    @Mock
    private ActuatorAccessListener actuatorAccessListener;

    @Mock
    private ServiceAccessListener serviceAccessListener;

    @Mock
    private SubscriptionListener notificationListener;

    private final String symbioteId = "symbioteId";
    private final String internalId = "internalId";
    private ResourceInfo resourceSensor;
    private ResourceInfo resourceObservation;
    private ResourceInfo resourceActuator;
    private ResourceInfo resourceService;
    private Observation observation;
    private List<Observation> observations;
    private Map<String, Value> serviceParameters;
    private Map<String, Map<String, Value>> actuatorParameters;

    @Before
    public void initializeData() throws IOException {
        resourceSensor = new ResourceInfo(symbioteId, internalId);
        resourceSensor.setType("Sensor");
        resourceObservation = new ResourceInfo();
        resourceObservation.setType("Observation");
        resourceActuator = new ResourceInfo(symbioteId, internalId);
        resourceActuator.setType("Actuator");
        resourceService = new ResourceInfo(symbioteId, internalId);
        resourceService.setType("Service");
        observation = new Observation(internalId, null, null, null, null);
        observations = Arrays.asList(observation, observation);
        Map<String, Value> parameter1 = new HashMap<>();
        parameter1.put("parameter_name_1", PrimitiveValue.create("parameter_value_1"));
        Map<String, Value> parameter2 = new HashMap<>();
        parameter1.put("parameter_name_2", PrimitiveValue.create("parameter_value_2"));
        serviceParameters = new HashMap<>();
        serviceParameters.putAll(parameter1);
        serviceParameters.putAll(parameter2);
        actuatorParameters = new HashMap<>();
        actuatorParameters.put("capability_1", parameter1);
        actuatorParameters.put("capability_2", parameter2);
    }

    private RapPlugin createRapPlugin() {
        return new RapPlugin(null, RAP_PLUGIN_ID, false, false);
    }

    @Test
    public void callingReadingResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        assertThatThrownBy(() -> {
            plugin.doReadResource(Arrays.asList(resourceSensor, resourceObservation));
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ResourceAccessListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void callingReadingResourceWhenUnregisteredAccessListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerReadingResourceListener(readingAccessListener);
        plugin.unregisterReadingResourceListener(readingAccessListener);
        assertThatThrownBy(() -> {
            plugin.doReadResource(Arrays.asList(resourceSensor, resourceObservation));
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ResourceAccessListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void registeringAccessListenerAndCallingReadingResource_shouldCallListener() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerReadingResourceListener(readingAccessListener);
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Observation.class, ObservationMixin.class);
        when(readingAccessListener.getResource(any()))
                .thenReturn(mapper.writeValueAsString(observation));

        String result = plugin.doReadResource(Arrays.asList(resourceSensor, resourceObservation));

        Observation resultObservation = new ObjectMapper().readValue(result, Observation.class);
        assertThat(resultObservation).isEqualTo(observation);
    }

    @Test
    public void callingReadingResourceHistoryWhenNotRegisteredListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        assertThatThrownBy(() -> {
            plugin.doReadResourceHistory(Arrays.asList(resourceSensor, resourceObservation), 1, null);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ResourceAccessListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void registeringResourceAccessListenerAndCallingReadingResourceHistory_shouldCallListener() throws Exception {
        RapPlugin plugin = createRapPlugin();
        ObjectMapper mapper = new ObjectMapper();
        String expectedResult = mapper.writeValueAsString(observations);
        when(readingAccessListener.getResourceHistory(
                any(),
                anyInt(),
                any()))
                .thenReturn(expectedResult);
        plugin.registerReadingResourceListener(readingAccessListener);
        String result = plugin.doReadResourceHistory(
                Arrays.asList(resourceSensor, resourceObservation),
                observations.size(),
                null);
        List<Observation> resultObservation = mapper.readValue(result, mapper.getTypeFactory().constructCollectionType(List.class, Observation.class));
        assertThat(resultObservation).isEqualTo(observations);
    }

    @Test
    public void callingWritingResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        assertThatThrownBy(() -> {
            plugin.doActuateResource(internalId, actuatorParameters);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ActuatorAccessListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void callingWritingResourceWhenUnregisteredActuatorAccessListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerActuatingResourceListener(actuatorAccessListener);
        plugin.unregisterActuatingResourceListener(actuatorAccessListener);
        assertThatThrownBy(() -> {
            plugin.doActuateResource(internalId, actuatorParameters);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ActuatorAccessListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void registeringActuatorAccessListenerAndCallingActuateResource_shouldCallListener() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerActuatingResourceListener(actuatorAccessListener);

        plugin.doActuateResource("resourceId", null);

        verify(actuatorAccessListener).actuateResource(any(), any());
    }

    @Test
    public void callingInvokeServiceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        assertThatThrownBy(() -> {
            plugin.doInvokeService(internalId, serviceParameters);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ServiceAccessListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void callingInvokeServiceWhenUnregisteredServiceAccessListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerInvokingServiceListener(serviceAccessListener);
        plugin.unregisterInvokingServiceListener(serviceAccessListener);
        assertThatThrownBy(() -> {
            plugin.doInvokeService(internalId, serviceParameters);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("ServiceAccessListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void registeringServiceAccessLisntenerAndCallingInvokingService_shouldCallListener() throws Exception {
        RapPlugin plugin = createRapPlugin();
        String expectedResultString = "{}";
        JsonNode expectedResultJson = new ObjectMapper().readTree(expectedResultString);

        plugin.registerInvokingServiceListener(serviceAccessListener);
        when(serviceAccessListener.invokeService(any(), any()))
                .thenReturn(expectedResultString);
        String result = plugin.doInvokeService(internalId, serviceParameters);
        JsonNode resultJson = new ObjectMapper().readTree(result);
        assertThat(resultJson).isEqualTo(expectedResultJson);
    }

    @Test
    public void callingSubcribeResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        assertThatThrownBy(() -> {
            plugin.doSubscribeResource(internalId);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("NotificationResourceListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void callingSubscribeResourceWhenUnregisteredListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerNotificationResourceListener(notificationListener);
        plugin.unregisterNotificationResourceListener(notificationListener);
        assertThatThrownBy(() -> {
            plugin.doSubscribeResource(internalId);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("NotificationResourceListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void registeringAndCallingSubscribeResource_shouldCallListener() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerNotificationResourceListener(notificationListener);
        plugin.doSubscribeResource(internalId);
        verify(notificationListener).subscribeResource(internalId);
    }

    @Test
    public void callingUnsubcribeResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        RapPlugin plugin = createRapPlugin();
        assertThatThrownBy(() -> {
            plugin.doUnsubscribeResource(internalId);
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessage("NotificationResourceListener not registered in RapPlugin")
                .hasNoCause();
    }

    @Test
    public void registeringAndCallingUnsubscribeResource_shouldCallListener() throws Exception {
        RapPlugin plugin = createRapPlugin();
        plugin.registerNotificationResourceListener(notificationListener);
        plugin.doUnsubscribeResource(internalId);
        verify(notificationListener).unsubscribeResource(internalId);
    }
}
