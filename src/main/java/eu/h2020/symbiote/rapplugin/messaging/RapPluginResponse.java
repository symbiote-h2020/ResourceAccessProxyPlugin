package eu.h2020.symbiote.rapplugin.messaging;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import eu.h2020.symbiote.rapplugin.messaging.rap.RapPluginException;

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RapPluginOkResponse.class),
        @JsonSubTypes.Type(value = RapPluginErrorResponse.class)
})
@JsonIgnoreProperties(value = { "content" })
public abstract class RapPluginResponse {
    @JsonProperty("responseCode")
    private int responseCode;
    
    public RapPluginResponse() {
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }
    
    public abstract String getContent() throws RapPluginException;

    @Override
    public int hashCode() {
        return Objects.hashCode(responseCode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof RapPluginResponse))
            return false;
        RapPluginResponse other = (RapPluginResponse) obj;
        return Objects.equals(responseCode, other.responseCode);
    }
}
