package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "rabbit", ignoreInvalidFields = true)
public class RabbitConnectionProperties {
    private String host = "localhost";
    private String username = "guest";
    private String password = "guest";
}


