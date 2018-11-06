package eu.h2020.symbiote.rapplugin;

import java.util.Arrays;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.h2020.symbiote.rapplugin.messaging.RabbitConfiguration;
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.rapplugin.properties.Properties;
import eu.h2020.symbiote.rapplugin.properties.RabbitProperties;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;

@Configuration
@EnableConfigurationProperties({
    RabbitProperties.class, 
    RapPluginProperties.class})
@Import({RabbitConfiguration.class})
public class RapPluginConfiguration implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(RapPluginConfiguration.class);
    
    @Value("${rabbit.replyTimeout}")
    private long rabbitReplyTimeout;
    
    @Bean
    public ConnectionFactory connectionFactory(RabbitProperties props) {
        CachingConnectionFactory factory = new CachingConnectionFactory(props.getHost());
        factory.setUsername(props.getUsername());
        factory.setPassword(props.getPassword());
		return factory;
    }

    @Bean
    public RabbitTemplate rabbitTemaplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setReplyTimeout(rabbitReplyTimeout);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }

    @Bean
    public RabbitAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        return rabbitAdmin;
    }
    
    @Bean
    public RabbitManager rapRabbitManager(RabbitTemplate template) {
    	return new RabbitManager(template);
    }
    
    @Bean
    public RapPlugin rapPlugin(RabbitManager manager, Properties properties) {
    	return new RapPlugin(manager, properties);
    }
    
    @Bean
    public Properties properties(RabbitProperties rabbitConnection, RapPluginProperties pluginProperties) {
    	return new Properties(rabbitConnection, pluginProperties);
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // print bean names in context
        LOG.debug("ALL BEANS: " + Arrays.toString(ctx.getBeanDefinitionNames()));
    }
}
