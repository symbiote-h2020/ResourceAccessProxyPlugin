package eu.h2020.symbiote.rapplugin.rap.plugin;

import static eu.h2020.symbiote.util.json.JsonPathAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.rabbitmq.client.Channel;

import eu.h2020.symbiote.rapplugin.EmbeddedRabbitFixture;
import eu.h2020.symbiote.rapplugin.TestingRabbitConfig;
import eu.h2020.symbiote.rapplugin.messaging.RabbitConfiguration;
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapDefinitions;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.rapplugin.properties.RabbitProperties;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;

@RunWith(SpringRunner.class)
@Import({TestingRabbitConfig.class,
    RapPluginProperties.class,
    RabbitConfiguration.class})
@EnableConfigurationProperties({RabbitProperties.class, RapPluginProperties.class})
@TestPropertySource("classpath:rabbitReplyTimeout.properties")
@DirtiesContext
public class RapPluginRegistrationTest extends EmbeddedRabbitFixture {
    private static final String PLUGIN_REGISTRATION_EXCHANGE = RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_OUT;
    private static final String PLUGIN_REGISTRATION_QUEUE_NAME = "test_plugin_registration";
    private static final String PLUGIN_REGISTRATION_KEY = RapDefinitions.PLUGIN_REGISTRATION_KEY;
    
    @org.springframework.beans.factory.annotation.Value("${rabbit.replyTimeout}")
    private int rabbitReceiveTimeout;
    
    @Configuration
    public static class TestConfiguration {
        @Bean
        public RapPlugin rapPlugin(RabbitManager manager) {
            return new RapPlugin(manager, "platId", false, true);
        }

        @Bean
        public RabbitManager rapRabbitManager(RabbitTemplate template) {
        	return new RabbitManager(template);
        }
    }
    
    @Autowired
    private RapPlugin rapPlugin;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory factory;
    
    @Autowired
    private RabbitAdmin rabbitAdmin;
    
    private Connection connection;
    private Channel channel;

    @Before
    public void initialize() throws Exception {
        connection = factory.createConnection();
        channel = connection.createChannel(false);

        cleanRabbitResources();
        createRabbitResources();
    }

    private void createRabbitResources() throws IOException {
        rabbitAdmin.declareExchange(ExchangeBuilder.topicExchange(PLUGIN_REGISTRATION_EXCHANGE).build());
        rabbitAdmin.declareQueue(QueueBuilder.durable(PLUGIN_REGISTRATION_QUEUE_NAME)
                .autoDelete()
                .withArgument("x-message-ttl", rabbitReceiveTimeout)
                .build());
        rabbitAdmin.purgeQueue(PLUGIN_REGISTRATION_QUEUE_NAME, true);
        rabbitAdmin.declareBinding(new Binding(PLUGIN_REGISTRATION_QUEUE_NAME, DestinationType.QUEUE, 
                PLUGIN_REGISTRATION_EXCHANGE, PLUGIN_REGISTRATION_KEY, null));
    }

    private void cleanRabbitResources() throws IOException {
        rabbitAdmin.deleteQueue(PLUGIN_REGISTRATION_QUEUE_NAME);
        rabbitAdmin.deleteExchange(PLUGIN_REGISTRATION_EXCHANGE);
    }


    @Test
    public void platformRegistration_shouldSendMessageToRapAtStartup() throws Exception {
        //given
    
        // when
        rapPlugin.start();
    
        //then
        Message message = rabbitTemplate.receive(PLUGIN_REGISTRATION_QUEUE_NAME, rabbitReceiveTimeout);
        assertNotNull(message);
        
        String jsonBody = new String(message.getBody(), StandardCharsets.UTF_8);
        DocumentContext ctx = JsonPath.parse(jsonBody);
        assertThat(ctx).jsonPathAsString("type").isEqualTo("REGISTER_PLUGIN");
        assertThat(ctx).jsonPathAsString("platformId").isEqualTo("platId");
        assertThat(ctx).jsonPathAsBoolean("hasFilters").isFalse();
        assertThat(ctx).jsonPathAsBoolean("hasNotifications").isTrue();
    }

    @Test
    public void platformUnregistration_shouldSendMessageToRapAtShoutdown() throws Exception {
        //given
    
        // when
        rapPlugin.stop();
    
        //then
        Message message = rabbitTemplate.receive(PLUGIN_REGISTRATION_QUEUE_NAME, rabbitReceiveTimeout);
        assertNotNull(message);
        
        String jsonBody = new String(message.getBody(), StandardCharsets.UTF_8);
        DocumentContext ctx = JsonPath.parse(jsonBody);
        assertThat(ctx).jsonPathAsString("type").isEqualTo("UNREGISTER_PLUGIN");
        assertThat(ctx).jsonPathAsString("platformId").isEqualTo("platId");
    }
}
