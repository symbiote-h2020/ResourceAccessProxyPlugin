package eu.h2020.symbiote.rapplugin;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import eu.h2020.symbiote.rapplugin.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.rapplugin.properties.RapProperties;


@Configuration
@ComponentScan(basePackages = {"eu.h2020.symbiote.rapplugin"})
@EnableConfigurationProperties({
    RabbitConnectionProperties.class, 
    RapProperties.class})
public class RapPluginConfiguration implements ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(RapPluginConfiguration.class);
    
    @Autowired
    private SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory;
    
    @PostConstruct
    public void initialize() {
        simpleRabbitListenerContainerFactory.setMessageConverter(new Jackson2JsonMessageConverter());
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        // print bean names in context
        LOG.debug("ALL BEANS: " + Arrays.toString(ctx.getBeanDefinitionNames()));
    }
}
