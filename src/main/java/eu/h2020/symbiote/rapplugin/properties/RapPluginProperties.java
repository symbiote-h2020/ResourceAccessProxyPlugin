package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * Properties for RapPlugin.
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
public class RapPluginProperties {
    /**
     * The name of RAP plugin. 
     * It reads 'spring.application.name' from properties.
     * It defaults to "DefaultRapPluginName".
     * 
     */
    @Getter
    @Value("${spring.application.name:DefaultRapPluginName}")
    private String pluginName = "";

    /**
     * RabbitMQ properties from <code>RabbitConnectionProperties</code> class
     * 
     * @see eu.h2020.symbiote.rapplugin.properties.RabbitConnectionProperties
     */
    @Getter
    private RabbitConnectionProperties rabbitConnection;
    
    private RapProperties pluginProperties;

    public RapPluginProperties() {
        rabbitConnection = new RabbitConnectionProperties();
        pluginProperties = new RapProperties();
    }

    @Autowired
    public RapPluginProperties(RabbitConnectionProperties rabbitConnection, RapProperties pluginProperties) {
        this.rabbitConnection = rabbitConnection;
        this.pluginProperties = pluginProperties;
    }

    /**
     * Getter for RapProperties.
     * @return RapProperties
     * 
     * @see eu.h2020.symbiote.rapplugin.properties.RapProperties
     */
    public RapProperties getPlugin() {
        return pluginProperties;
    }
}
