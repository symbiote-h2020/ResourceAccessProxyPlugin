package eu.h2020.symbiote.rapplugin.rap.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.paweladamski.httpclientmock.HttpClientMock;
import com.rabbitmq.client.Channel;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;

import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Capability;
import eu.h2020.symbiote.rapplugin.EmbeddedRabbitFixture;
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginErrorResponse;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginOkResponse;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginResponse;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapDefinitions;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPluginException;
import eu.h2020.symbiote.rapplugin.properties.RabbitProperties;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;
import eu.h2020.symbiote.rapplugin.value.Value;
import eu.h2020.symbiote.rapplugin.TestingRabbitConfig;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.rapplugin.CapabilityDeserializer;
import eu.h2020.symbiote.rapplugin.ParameterDeserializer;
import eu.h2020.symbiote.rapplugin.messaging.rap.ActuatorAccessListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.ResourceAccessListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.ServiceAccessListener;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import({RabbitManager.class,
    TestingRabbitConfig.class,
    RapPluginProperties.class})
@EnableConfigurationProperties({RabbitProperties.class, RapPluginProperties.class})
@DirtiesContext
/**
 * @author Mario Kušek <mario.kusek@fer.hr>
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class RapPluginAccessTest extends EmbeddedRabbitFixture {

    private static final Logger LOG = LoggerFactory.getLogger(RapPluginAccessTest.class);

    private static final String RAP_PLUGIN_ID = "rap_plugin_test";
    // RabbitMQ
    private static final String RAP_QUEUE_NAME = RAP_PLUGIN_ID;
    private static final String PLUGIN_REGISTRATION_EXCHANGE = RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_OUT;
    private static final String PLUGIN_EXCHANGE = RapDefinitions.PLUGIN_EXCHANGE_IN;
    private static final String RABBIT_ROUTING_KEY_GET = RAP_PLUGIN_ID + ".get";
    private static final String RABBIT_ROUTING_KEY_SET = RAP_PLUGIN_ID + ".set";
    private static final String RABBIT_ROUTING_KEY_HISTORY = RAP_PLUGIN_ID + ".history";

    private static final int RECEIVE_TIMEOUT = 20_000;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RapPlugin rapPlugin(RabbitManager manager) {
            return new RapPlugin(manager, RAP_PLUGIN_ID, false, true);
        }
    }

    @Autowired
    private RapPlugin rapPlugin;

    @Autowired
    private ConnectionFactory factory;

    private Connection connection;
    private Channel channel;
    private ObjectMapper mapper;

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Before
    public void initialize() throws Exception {
        LOG.debug("All beans - names: {}", String.join(", ", ctx.getBeanDefinitionNames()));
        initializeJacksonMapper();
        initializeRabbitResources();
        initializeData();
    }

    private final String symbioteId = "symbioteId";
    private final String internalId = "internalId";
    private ResourceInfo resourceSensor;
    private ResourceInfo resourceActuator;
    private CloudResource cloudResourceActuator;
    private ResourceInfo resourceService;
    private CloudResource cloudResourceService;
    private Observation observation;
    private List<Observation> observations;
    private ResourceAccessGetMessage getMessage;
    private ResourceAccessHistoryMessage historyMessage;
    private List<Map<String, Object>> serviceParameters;
    private Map<String, List<Map<String, Object>>> actuatorParameters;

    private CloudResource createServiceStub(String internalId, List<Parameter> parameters) throws InvalidArgumentsException {
        CloudResource result = new CloudResource();

        return result;
    }

    private void initializeData() throws InvalidArgumentsException {
        // parameter 
        Map<String, Object> parameter1 = new HashMap<>();
        parameter1.put("parameter_name_1", "parameter_value_1");
        Map<String, Object> parameter2 = new HashMap<>();
        parameter2.put("parameter_name_2", "parameter_value_2");
        PrimitiveDatatype xsdString = new PrimitiveDatatype();
        xsdString.setBaseDatatype("xsd:string");
        Parameter parameter1Definition = new Parameter();
        parameter1Definition.setName("parameter_name_1");
        parameter1Definition.setMandatory(true);
        parameter1Definition.setDatatype(xsdString);
        Parameter parameter2Definition = new Parameter();
        parameter2Definition.setName("parameter_name_2");
        parameter2Definition.setMandatory(false);
        parameter2Definition.setDatatype(xsdString);
        List<Parameter> parameterDefinitions = Arrays.asList(parameter1Definition, parameter2Definition);
        // sensor
        resourceSensor = new ResourceInfo(symbioteId, internalId);
        resourceSensor.setType("Sensor");
        // actuator
        resourceActuator = new ResourceInfo(symbioteId, internalId);
        resourceActuator.setType("Actuator");
        actuatorParameters = new HashMap<>();
        actuatorParameters.put("capability_1", Arrays.asList(parameter1));
        actuatorParameters.put("capability_2", Arrays.asList(parameter2));
        cloudResourceActuator = new CloudResource();
        cloudResourceActuator.setInternalId(internalId);
        cloudResourceActuator.setPluginId(RAP_PLUGIN_ID);
        cloudResourceActuator.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
        cloudResourceActuator.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
        String interworkingInterfaceUrl = "foo";
        Actuator actuator = new Actuator();
        actuator.setName("test service " + internalId);
        actuator.setDescription(Arrays.asList("test service " + internalId));
        actuator.setInterworkingServiceURL(interworkingInterfaceUrl);
        Capability capability1 = new Capability();
        capability1.setName("capability_1");
        capability1.setParameters(Arrays.asList(parameter1Definition));
        Capability capability2 = new Capability();
        capability2.setName("capability_2");
        capability2.setParameters(Arrays.asList(parameter2Definition));
        actuator.setCapabilities(Arrays.asList(capability1, capability2));
        cloudResourceActuator.setResource(actuator);
        // service
        resourceService = new ResourceInfo(symbioteId, internalId);
        resourceService.setType("Service");
        serviceParameters = Arrays.asList(parameter1, parameter2);
        cloudResourceService = new CloudResource();
        cloudResourceService.setInternalId(internalId);
        cloudResourceService.setPluginId(RAP_PLUGIN_ID);
        cloudResourceService.setAccessPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
        cloudResourceService.setFilteringPolicy(new SingleTokenAccessPolicySpecifier(AccessPolicyType.PUBLIC, null));
        Service service = new Service();
        service.setName("test service " + internalId);
        service.setDescription(Arrays.asList("test service " + internalId));
        service.setInterworkingServiceURL(interworkingInterfaceUrl);
        service.setParameters(parameterDefinitions);
        cloudResourceService.setResource(service);
//        cloudResourceService.s 
        observation = new Observation(internalId, null, null, null, null);
        observations = Arrays.asList(observation, observation);
        getMessage = new ResourceAccessGetMessage(Arrays.asList(resourceSensor));
        historyMessage = new ResourceAccessHistoryMessage(Arrays.asList(resourceSensor), observations.size(), null);
    }

    private void initializeJacksonMapper() {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    private void initializeRabbitResources() throws Exception {
        rabbitTemplate.setReplyTimeout(RECEIVE_TIMEOUT);
        connection = factory.createConnection();
        channel = connection.createChannel(false);
        createRabbitResources();
    }

    private void createRabbitResources() throws IOException {
        rabbitAdmin.declareExchange(ExchangeBuilder.topicExchange(PLUGIN_REGISTRATION_EXCHANGE).build());
        rabbitAdmin.declareExchange(ExchangeBuilder.topicExchange(PLUGIN_EXCHANGE).build());
        rabbitAdmin.declareQueue(QueueBuilder.durable(RAP_QUEUE_NAME).build());
        rabbitAdmin.purgeQueue(RAP_QUEUE_NAME, false);
    }

    @After
    public void teardownRabbitResources() throws Exception {
        cleanRabbitResources();
    }

    private void cleanRabbitResources() throws IOException {
        channel.queueDelete(RAP_QUEUE_NAME);
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessGetMessage_whenExceptionInPlugin_shouldReturnEmptyList() throws Exception {
        ResourceAccessListener listener = Mockito.mock(ResourceAccessListener.class);
        when(listener.getResource(any()))
                .thenThrow(new RuntimeException("exception message"));
        rapPlugin.registerReadingResourceListener(listener);

        String message = mapper.writeValueAsString(getMessage);
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_GET, message);
        assertNotNull(response);
        assertThat(response).isInstanceOf(RapPluginErrorResponse.class);
        RapPluginErrorResponse errorResponse = (RapPluginErrorResponse) response;
        assertThat(errorResponse.getResponseCode()).isEqualTo(500);
        assertThat(errorResponse.getMessage()).isNotEmpty();
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessGetMessage_shouldReturnResourceReading() throws Exception {
        ResourceAccessListener listener = Mockito.mock(ResourceAccessListener.class);
        when(listener.getResource(any()))
                .thenReturn(mapper.writeValueAsString(observation));
        rapPlugin.registerReadingResourceListener(listener);
        String message = mapper.writeValueAsString(getMessage);
        Message request = rabbitTemplate.getMessageConverter().toMessage(message, new MessageProperties());
        Message response = rabbitTemplate.sendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_GET, request);
        assertThat(response).isNotNull();
        RapPluginOkResponse okResponse = new ObjectMapper().readValue(response.getBody(), new TypeReference<RapPluginOkResponse>() {
        });
        assertThat(okResponse.getBody()).isInstanceOf(String.class);
        Observation returnedObservation = mapper.readValue(okResponse.getBody().toString(), Observation.class);
        assertThat(returnedObservation).isNotNull();
        assertThat(returnedObservation.getResourceId()).isEqualTo(internalId);
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessHistoryMessage_whenExceptionInPlugin_shouldReturnHistoryOfResourceReading() throws Exception {
        ResourceAccessListener listener = Mockito.mock(ResourceAccessListener.class);
        when(listener.getResourceHistory(any(), anyInt(), any()))
                .thenThrow(new RuntimeException("exception message"));
        rapPlugin.registerReadingResourceListener(listener);
        String message = mapper.writeValueAsString(historyMessage);
        Object respsonse = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_HISTORY, message);
        assertNotNull(respsonse);
        assertThat(respsonse).isInstanceOf(RapPluginErrorResponse.class);
        RapPluginErrorResponse errResponse = (RapPluginErrorResponse) respsonse;
        assertThat(errResponse.getResponseCode()).isEqualTo(500);
        assertThat(errResponse.getMessage()).isNotEmpty();
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessHistoryMessage_shouldReturnHistoryOfResourceReading() throws Exception {
        ResourceAccessListener listener = Mockito.mock(ResourceAccessListener.class);
        when(listener.getResourceHistory(any(), anyInt(), any()))
                .thenReturn(mapper.writeValueAsString(observations));
        rapPlugin.registerReadingResourceListener(listener);
        String message = mapper.writeValueAsString(historyMessage);
        Message request = rabbitTemplate.getMessageConverter().toMessage(message, new MessageProperties());
        Message response = rabbitTemplate.sendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_HISTORY, request);
        assertNotNull(response);
        RapPluginOkResponse okResponse = mapper.readValue(response.getBody(), RapPluginOkResponse.class);
        assertThat(okResponse.getBody()).isInstanceOf(String.class);
        List<Observation> returnedObservations = mapper.readValue(okResponse.getBody().toString(), TypeFactory.defaultInstance().constructCollectionType(List.class, Observation.class));
        assertThat(returnedObservations)
                .isNotNull()
                .hasSize(2)
                .extracting(Observation::getResourceId)
                .contains(internalId, internalId);
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessSetMessageForActuation_whenExceptionInPlugin_shouldReturnNull() throws Exception {
        // given
        ActuatorAccessListener listener = Mockito.mock(ActuatorAccessListener.class);
        doThrow(new RuntimeException("exception message"))
            .when(listener).actuateResource(anyString(), anyMap());
        
        rapPlugin.registerActuatingResourceListener(listener);
        String parameters = mapper.writeValueAsString(actuatorParameters);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(Arrays.asList(resourceActuator), parameters);
        String message = mapper.writeValueAsString(msg);
        
        // when
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_SET, message);
        
        // then
        assertThat(response).isInstanceOf(RapPluginErrorResponse.class);
        RapPluginErrorResponse errResponse = (RapPluginErrorResponse) response;
        assertThat(errResponse.getResponseCode()).isEqualTo(500);
    }      

    @Test @DirtiesContext
    public void sendingResourceAccessActuation_shouldReturn204() throws Exception {
        //given
        ActuatorAccessListener writingListener = Mockito.mock(ActuatorAccessListener.class);
        
        rapPlugin.registerActuatingResourceListener(writingListener);
        
        ResourceInfo resourceInfo = new ResourceInfo(symbioteId, internalId);
        resourceInfo.setType("Actuator");
        List<ResourceInfo> infoList = Arrays.asList(resourceInfo);
        String body = mapper.writeValueAsString(actuatorParameters); //createCapabilityRabbitMessage(parameterList);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);
        String json = mapper.writeValueAsString(msg);
        
        // when
        Object returnedObject = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_SET, json);          
    
        // then
        assertThat(returnedObject).isInstanceOf(RapPluginOkResponse.class);
        RapPluginResponse response = (RapPluginOkResponse) returnedObject;
        assertThat(response.getResponseCode()).isEqualTo(204);
        
        ArgumentCaptor<Map>  argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(writingListener).actuateResource(eq(internalId), argumentCaptor.capture());
        Map<String, Map<String, Value>> actualCapabilities = argumentCaptor.getValue();

        assertThat(actualCapabilities).containsOnlyKeys(actuatorParameters.keySet().toArray(new String[actuatorParameters.size()]));
        assertCapability(actualCapabilities, "capability_1", "parameter_name_1", "parameter_value_1");
        assertCapability(actualCapabilities, "capability_2", "parameter_name_2", "parameter_value_2");
    }

    @Test @DirtiesContext
    public void sendingResourceAccessActuation_whenTypeIsLight_shouldReturn204() throws Exception {
        //given
        ActuatorAccessListener writingListener = Mockito.mock(ActuatorAccessListener.class);
        
        rapPlugin.registerActuatingResourceListener(writingListener);
        
        ResourceInfo resourceInfo = new ResourceInfo(symbioteId, internalId);
        resourceInfo.setType("Light");
        List<ResourceInfo> infoList = Arrays.asList(resourceInfo);
        String body = mapper.writeValueAsString(actuatorParameters); //createCapabilityRabbitMessage(parameterList);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);
        String json = mapper.writeValueAsString(msg);
        
        // when
        Object returnedObject = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_SET, json);          
    
        // then
        assertThat(returnedObject).isInstanceOf(RapPluginOkResponse.class);
        RapPluginResponse response = (RapPluginOkResponse) returnedObject;
        assertThat(response.getResponseCode()).isEqualTo(204);
        
        ArgumentCaptor<Map>  argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(writingListener).actuateResource(eq(internalId), argumentCaptor.capture());
        Map<String, Map<String, Value>> actualCapabilities = argumentCaptor.getValue();

        assertThat(actualCapabilities).containsOnlyKeys(actuatorParameters.keySet().toArray(new String[actuatorParameters.size()]));
        assertCapability(actualCapabilities, "capability_1", "parameter_name_1", "parameter_value_1");
        assertCapability(actualCapabilities, "capability_2", "parameter_name_2", "parameter_value_2");
    }

    @Test @DirtiesContext
    public void sendingResourceAccessActuation_whenExceptionInPlugin_shouldReturnNull() throws Exception {
        //given
        ActuatorAccessListener writingListener = Mockito.mock(ActuatorAccessListener.class);
        doThrow(new RuntimeException("exception message")).when(writingListener).actuateResource(any(), any());
        
        rapPlugin.registerActuatingResourceListener(writingListener);
        
        ResourceInfo resourceInfo = new ResourceInfo(symbioteId, internalId);
        resourceInfo.setType("Actuator");
        List<ResourceInfo> infoList = Arrays.asList(resourceInfo);
        String body = mapper.writeValueAsString(actuatorParameters); //createCapabilityRabbitMessage(parameterList);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);
        String json = mapper.writeValueAsString(msg);
        
        // when
        Object returnedObject = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_SET, json);          
    
        // then
        assertThat(returnedObject).isInstanceOf(RapPluginErrorResponse.class);
        RapPluginErrorResponse response = (RapPluginErrorResponse) returnedObject;
        assertThat(response.getResponseCode()).isEqualTo(500);
    }
    
    @Test
    @DirtiesContext
    public void sendingResourceAccessInvokeService_whenExceptionInPlugin_shouldError() throws Exception {
        // given
        ServiceAccessListener listener = Mockito.mock(ServiceAccessListener.class);

        when(listener.invokeService(any(), any()))
                .thenThrow(new RapPluginException(500, "Some Internal Test Error"));
        rapPlugin.registerInvokingServiceListener(listener);

        String parameters = mapper.writeValueAsString(serviceParameters);
        ResourceAccessSetMessage setMessage = new ResourceAccessSetMessage(Arrays.asList(resourceService), parameters);
        String message = mapper.writeValueAsString(setMessage);
        
        // when
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_SET, message);
        
        // then
        assertThat(response).isInstanceOf(RapPluginErrorResponse.class);
        RapPluginErrorResponse errorResponse = (RapPluginErrorResponse) response;
        assertThat(errorResponse.getResponseCode()).isEqualTo(500);
        assertThat(errorResponse.getMessage()).startsWith("Some Internal Test Error");
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessInvokingService_shouldReturnResult() throws Exception {
        ServiceAccessListener listener = Mockito.mock(ServiceAccessListener.class);

        String expectedServiceResult = "result";
        when(listener.invokeService(any(), any()))
                .thenReturn(expectedServiceResult);
        rapPlugin.registerInvokingServiceListener(listener);

        HttpClientMock httpClientMock = new HttpClientMock();
        httpClientMock
                .onGet()
                .withParameter("resourceInternalId", containsString(""))
                .doReturnJSON(mapper.writeValueAsString(cloudResourceService));

        String parameters = mapper.writeValueAsString(serviceParameters);
        ResourceAccessSetMessage setMessage = new ResourceAccessSetMessage(Arrays.asList(resourceService), parameters);
        String message = mapper.writeValueAsString(setMessage);
        Message request = rabbitTemplate.getMessageConverter().toMessage(message, new MessageProperties());
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, RABBIT_ROUTING_KEY_SET, request);
        assertThat(response).isInstanceOf(RapPluginOkResponse.class);
        RapPluginOkResponse okResponse = (RapPluginOkResponse) response;
        assertThat(okResponse.getResponseCode()).isEqualTo(200);
        assertThat(okResponse.getBody()).isEqualTo(expectedServiceResult);
    }
    
    private void assertCapability(Map<String, Map<String, Value>> actualCapabilities, String capabilityName, String parameterName,
            String parameterValue) {
        assertThat(actualCapabilities).containsKey(capabilityName);
        Map<String, Value> parameters = actualCapabilities.get(capabilityName);
        assertThat(parameters).containsKey(parameterName);
        assertThat(parameters.get(parameterName).get()).isEqualTo(parameterValue);
    }
}
