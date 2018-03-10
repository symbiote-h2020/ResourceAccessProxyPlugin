package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Properties for RabbitMQ connection.
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "rabbit", ignoreInvalidFields = true)
public class RabbitConnectionProperties {
    /**
     * Host of RabbitMQ server. 
     * Property: <code>rabbit.host</code>. 
     * Default value is <code>localhost</code>.
     */
    private String host = "localhost";

    /**
     * Username for connecting to RabbitMQserver. 
     * Property: <code>rabbit.username</code>. 
     * Default value is <code>guest</code>.
     */
    private String username = "guest";

    /**
     * Password for connecting to RabbitMQserver. 
     * Property: <code>rabbit.password</code>. 
     * Default value is <code>guest</code>.
     */
    private String password = "guest";
}


