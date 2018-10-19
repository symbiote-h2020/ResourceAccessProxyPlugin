package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RapProperties
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
@Data
@Component
@NoArgsConstructor
@ConfigurationProperties(prefix = "rap-plugin")
public class RapPluginProperties {
    /**
     * Defines if RAP plugin is able do handle OData filters.
     * 
     * Property: <code>rap-plugin.filtersSupported</code>. 
     * Default value is <code>false</code>.
     */
    private boolean filtersSupported = false;
    
    
    /**
     * Defines if RAP plugin is able do handle notification subscriptions.
     * 
     * Property: <code>rap-plugin.notificationsSupported</code>. 
     * Default value is <code>false</code>.
     */
    private boolean notificationsSupported = false;
    
    /**
     * URL of registration handler.
     * 
     * Property: <code>rap-plugin.registrationHandlerUrl</code>. 
     * Default value is <code>http://localhost/rh</code>.
     */
    private String registrationHandlerUrl = "http://localhost/rh";
}