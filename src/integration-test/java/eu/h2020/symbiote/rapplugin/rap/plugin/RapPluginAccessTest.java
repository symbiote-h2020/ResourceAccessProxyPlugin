package eu.h2020.symbiote.rapplugin.rap.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.rabbitmq.client.Channel;

import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.db.ResourceInfo;
import eu.h2020.symbiote.rapplugin.EmbeddedRabbitFixture;
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginErrorResponse;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginOkResponse;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapDefinitions;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPluginException;
import eu.h2020.symbiote.rapplugin.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;
import eu.h2020.symbiote.rapplugin.properties.RapProperties;
import eu.h2020.symbiote.rapplugin.TestingRabbitConfig;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.rapplugin.messaging.rap.ActuatorAccessListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.ResourceAccessListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.ServiceAccessListener;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;

@RunWith(SpringRunner.class)
@Import({RabbitManager.class,
    TestingRabbitConfig.class,
    RapPluginProperties.class})
@EnableConfigurationProperties({RabbitConnectionProperties.class, RapProperties.class})
@DirtiesContext
/**
 * @author Mario Kušek <mario.kusek@fer.hr>
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class RapPluginAccessTest extends EmbeddedRabbitFixture {

    private static final Logger LOG = LoggerFactory.getLogger(RapPluginAccessTest.class);
    private static final String PLUGIN_REGISTRATION_EXCHANGE = RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_OUT;
    private static final String PLUGIN_EXCHANGE = RapDefinitions.PLUGIN_EXCHANGE_IN;
    private static final String RAP_QUEUE_NAME = "test_rap";

    private static final int RECEIVE_TIMEOUT = 20_000;

    @Configuration
    public static class TestConfiguration {

        @Bean
        public RapPlugin rapPlugin(RabbitManager manager) {
            return new RapPlugin(manager, "enablerName", false, true);
        }
    }

    @Autowired
    private RapPlugin rapPlugin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

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
    private ResourceInfo resourceService;
    private Observation observation;
    private List<Observation> observations;
    private ResourceAccessGetMessage getMessage;
    private ResourceAccessHistoryMessage historyMessage;
    private List<Map<String, Object>> serviceParameters;
    private Map<String, Map<String, Object>> actuatorParameters;

    private void initializeData() {
        resourceActuator = new ResourceInfo(symbioteId, internalId);
        resourceActuator.setType("Sensor");
        resourceActuator = new ResourceInfo(symbioteId, internalId);
        resourceActuator.setType("Actuator");
        resourceService = new ResourceInfo(symbioteId, internalId);
        resourceService.setType("Service");
        observation = new Observation(internalId, null, null, null, null);
        observations = Arrays.asList(observation, observation);
        getMessage = new ResourceAccessGetMessage(Arrays.asList(resourceSensor));
        historyMessage = new ResourceAccessHistoryMessage(Arrays.asList(resourceSensor), observations.size(), null);
        Map<String, Object> parameter1 = new HashMap<>();
        parameter1.put("parameter_name_1", "parameter_value_1");
        Map<String, Object> parameter2 = new HashMap<>();
        parameter2.put("parameter_name_2", "parameter_value_2");
        serviceParameters = Arrays.asList(parameter1, parameter2);
        actuatorParameters = new HashMap<>();
        actuatorParameters.put("capability_1", parameter1);
        actuatorParameters.put("capability_2", parameter2);
    }

    private void initializeJacksonMapper() {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
    }

    private void initializeRabbitResources() throws Exception {
        rabbitTemplate.setReceiveTimeout(RECEIVE_TIMEOUT);
        connection = factory.createConnection();
        channel = connection.createChannel(false);
        createRabbitResources();
    }

    private void createRabbitResources() throws IOException {
        rabbitAdmin.declareExchange(ExchangeBuilder.topicExchange(PLUGIN_REGISTRATION_EXCHANGE).build());
        rabbitAdmin.declareExchange(ExchangeBuilder.topicExchange(PLUGIN_EXCHANGE).build());
        rabbitAdmin.declareQueue(QueueBuilder.durable(RAP_QUEUE_NAME).build());
        rabbitAdmin.purgeQueue(RAP_QUEUE_NAME, true);
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
        String routingKey = "enablerName.get";
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, message);
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
        String routingKey = "enablerName.get";
        Message request = rabbitTemplate.getMessageConverter().toMessage(message, new MessageProperties());
        Message response = rabbitTemplate.sendAndReceive(PLUGIN_EXCHANGE, routingKey, request);
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
                .thenThrow(new RuntimeException("excaption message"));
        rapPlugin.registerReadingResourceListener(listener);
        String message = mapper.writeValueAsString(historyMessage);
        String routingKey = "enablerName.history";
        Object respsonse = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, message);
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
        String routingKey = "enablerName.history";
        Message request = rabbitTemplate.getMessageConverter().toMessage(message, new MessageProperties());
        Message response = rabbitTemplate.sendAndReceive(PLUGIN_EXCHANGE, routingKey, request);
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
        ActuatorAccessListener listener = Mockito.mock(ActuatorAccessListener.class);
        doThrow(new RuntimeException("exception message"))
                .when(listener).actuateResource(anyString(), anyMap());
        rapPlugin.registerActuatingResourceListener(listener);
        String parameters = mapper.writeValueAsString(actuatorParameters);
        ResourceAccessSetMessage msg = new ResourceAccessSetMessage(Arrays.asList(resourceActuator), parameters);
        String message = mapper.writeValueAsString(msg);
        String routingKey = "enablerName.set";
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, message);
        assertThat(response).isInstanceOf(RapPluginErrorResponse.class);
        RapPluginErrorResponse errResponse = (RapPluginErrorResponse) response;
        assertThat(errResponse.getResponseCode()).isEqualTo(500);
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessInvokeService_whenExceptionInPlugin_shouldError() throws Exception {
        ServiceAccessListener listener = Mockito.mock(ServiceAccessListener.class);

        when(listener.invokeService(any(), any()))
                .thenThrow(new RapPluginException(500, "Some Internal Error"));
        rapPlugin.registerInvokingServiceListener(listener);

        String parameters = mapper.writeValueAsString(serviceParameters);
        ResourceAccessSetMessage setMessage = new ResourceAccessSetMessage(Arrays.asList(resourceService), parameters);
        String message = mapper.writeValueAsString(setMessage);
        String routingKey = "enablerName.set";
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, message);
        assertThat(response).isInstanceOf(RapPluginErrorResponse.class);
        RapPluginErrorResponse errorResponse = (RapPluginErrorResponse) response;
        assertThat(errorResponse.getResponseCode()).isEqualTo(500);
        assertThat(errorResponse.getMessage()).isEqualTo("Some Internal Error");
    }

    @Test
    @DirtiesContext
    public void sendingResourceAccessInvokingService_shouldReturnResult() throws Exception {
        ServiceAccessListener listener = Mockito.mock(ServiceAccessListener.class);

        String expectedServiceResult = "result";
        when(listener.invokeService(any(), any()))
                .thenReturn(expectedServiceResult);
        rapPlugin.registerInvokingServiceListener(listener);

        String parameters = mapper.writeValueAsString(serviceParameters);
        ResourceAccessSetMessage setMessage = new ResourceAccessSetMessage(Arrays.asList(resourceService), parameters);
        String message = mapper.writeValueAsString(setMessage);
        String routingKey = "enablerName.set";
        Message request = rabbitTemplate.getMessageConverter().toMessage(message, new MessageProperties());
        Object response = rabbitTemplate.convertSendAndReceive(PLUGIN_EXCHANGE, routingKey, request);
        assertThat(response).isInstanceOf(RapPluginOkResponse.class);
        RapPluginOkResponse okResponse = (RapPluginOkResponse) response;
        assertThat(okResponse.getResponseCode()).isEqualTo(200);
        assertThat(okResponse.getBody()).isEqualTo(expectedServiceResult);
    }
}
