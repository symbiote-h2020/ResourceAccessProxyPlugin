package eu.h2020.symbiote.rapplugin.messaging.properties;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.rapplugin.properties.RabbitConnectionProperties;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;
import eu.h2020.symbiote.rapplugin.properties.RapProperties;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties({
    RabbitConnectionProperties.class,
    RapProperties.class})
@TestPropertySource(locations = "classpath:empty.properties")
@Import(RapPluginProperties.class)
public class DefaultPropertiesTests {
    @Autowired
    private RapPluginProperties props;

    @Test
    public void shouldHaveLoadedProperties() {
        assertThat(props.getPlugin()).isNotNull();
        assertThat(props.getRabbitConnection()).isNotNull();
    }

    @Test
    public void shouldHaveEnablerName() throws Exception {
        assertThat(props.getEnablerName()).isEqualTo("DefaultRapPluginName");
    }

    @Test
    public void shouldHaveLoadedPluginProperties() throws Exception {
        assertThat(props.getPlugin().isFiltersSupported()).isFalse();
        assertThat(props.getPlugin().isNotificationsSupported()).isFalse();
    }
}
