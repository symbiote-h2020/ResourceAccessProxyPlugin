package eu.h2020.symbiote.rapplugin.rap.plugin;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Location;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public interface ObservationMixin {

    @JsonProperty
    public String getResourceId();

    @JsonProperty
    public Location getLocation();

    @JsonProperty
    public String getResultTime();

    @JsonProperty
    public String getSamplingTime();
}
