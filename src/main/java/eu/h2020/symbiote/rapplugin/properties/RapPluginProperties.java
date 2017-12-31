package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component("rapPluginProperties")
public class RapPluginProperties {
    @Getter
    @Value("${spring.application.name:DefaultRapPluginName}")
    private String pluginName = "";

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

    public RapProperties getPlugin() {
        return pluginProperties;
    }
}
