package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;

/**
 * Properties for RapPlugin.
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
public class Properties {
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
     * @see eu.h2020.symbiote.rapplugin.properties.RabbitProperties
     */
    @Getter
    private RabbitProperties rabbitConnection;
    
    private RapPluginProperties pluginProperties;

    public Properties() {
        rabbitConnection = new RabbitProperties();
        pluginProperties = new RapPluginProperties();
    }

    @Autowired
    public Properties(RabbitProperties rabbitConnection, RapPluginProperties pluginProperties) {
        this.rabbitConnection = rabbitConnection;
        this.pluginProperties = pluginProperties;
    }

    /**
     * Getter for RapProperties.
     * @return RapProperties
     * 
     * @see eu.h2020.symbiote.rapplugin.properties.RapPluginProperties
     */
    public RapPluginProperties getPlugin() {
        return pluginProperties;
    }
}
