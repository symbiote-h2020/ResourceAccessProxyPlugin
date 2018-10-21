package eu.h2020.symbiote.rapplugin.messaging;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPluginException;

public class RapPluginOkResponse extends RapPluginResponse {
    private Object body;
    
    public RapPluginOkResponse() {
        setResponseCode(204);
    }
    
    public RapPluginOkResponse(Object body) throws RapPluginException {
        setBody(body);
        if(body == null)
            setResponseCode(204);
        else
            setResponseCode(200);
    }
    
    public RapPluginOkResponse(int responseCode, Object body) throws RapPluginException {
        setBody(body);
        setResponseCode(responseCode);
    }
    
    @Override
    public void setResponseCode(int responseCode) {
        if(responseCode < 200 || responseCode > 299)
            throw new IllegalArgumentException("Response code should be in range from 200-299");
        super.setResponseCode(responseCode);
    }

    public void setBody(Object body) throws RapPluginException {
        if(body instanceof LinkedHashMap || body instanceof List)
           tryToParseObservationOrListOfObsrvation(body);
        else 
            this.body = body;
        getContent();
    }
    
    private void tryToParseObservationOrListOfObsrvation(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            body = mapper.convertValue(object, Observation.class);
            return;
        } catch (IllegalArgumentException e) {
            try {
                body = mapper.convertValue(object, new TypeReference<List<Observation>>() {});
                return;
            } catch (IllegalArgumentException e1) {
                body = object;
            }
        }
    }

    public Object getBody() {
        return body;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(body) + super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof RapPluginOkResponse))
            return false;
        RapPluginOkResponse other = (RapPluginOkResponse) obj;
        
        return super.equals(other) && Objects.equals(body, other.body);
    }

    @Override
    public String getContent() throws RapPluginException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                    "Content of body can not be serialized to JSON in RAP plugin. Body is of type " + 
                    body.getClass().getName(), e);
        }
    }
}
