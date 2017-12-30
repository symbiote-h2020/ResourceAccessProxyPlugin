package eu.h2020.symbiote.rapplugin.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "rap-plugin")
public class RapProperties {
    private boolean filtersSupported = false;
    private boolean notificationsSupported = false;
}