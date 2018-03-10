package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RapProperties
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "rap-plugin")
public class RapProperties {
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
}