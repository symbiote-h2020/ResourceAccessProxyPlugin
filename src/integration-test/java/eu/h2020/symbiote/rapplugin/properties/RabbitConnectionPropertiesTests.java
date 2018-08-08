package eu.h2020.symbiote.rapplugin.properties;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import eu.h2020.symbiote.rapplugin.properties.RabbitProperties;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties(RabbitProperties.class)
@TestPropertySource(locations = "classpath:custom.properties")
public class RabbitConnectionPropertiesTests {
    @Autowired
    private RabbitProperties props;

    @Value("${rabbit.host}")
    private String host;

    @Test
    public void shouldLoadFirstLevelProperties() {
        assertThat(props.getHost()).isEqualTo("127.0.0.1");
        assertThat(props.getUsername()).isEqualTo("u");
        assertThat(props.getPassword()).isEqualTo("p");
    }
}
