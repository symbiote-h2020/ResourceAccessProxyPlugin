package eu.h2020.symbiote.rapplugin.properties;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.rapplugin.properties.RabbitProperties;
import eu.h2020.symbiote.rapplugin.properties.Properties;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@EnableConfigurationProperties({
    RabbitProperties.class,
    RapPluginProperties.class})
@TestPropertySource(locations = "classpath:empty.properties")
@Import(Properties.class)
public class DefaultPropertiesTests {
    @Autowired
    private Properties props;

    @Test
    public void shouldHaveLoadedProperties() {
        assertThat(props.getPlugin()).isNotNull();
        assertThat(props.getRabbitConnection()).isNotNull();
    }

    @Test
    public void shouldHaveEnablerName() throws Exception {
        assertThat(props.getPluginName()).isEqualTo("DefaultRapPluginName");
    }

    @Test
    public void shouldHaveLoadedPluginProperties() throws Exception {
        assertThat(props.getPlugin().isFiltersSupported()).isFalse();
        assertThat(props.getPlugin().isNotificationsSupported()).isFalse();
    }
}
