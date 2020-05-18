package eu.h2020.symbiote.rapplugin.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(RapPluginProperties.class)
@TestPropertySource(locations = "classpath:empty.properties")
public class RapDefaultPropertiesTests {
    @Autowired
    private RapPluginProperties props;

    @Test
    public void shouldLoadFirstLevelProperties() {
        assertThat(props.isFiltersSupported()).isFalse();
        assertThat(props.isNotificationsSupported()).isFalse();
    }
}
