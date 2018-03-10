package eu.h2020.symbiote.rapplugin.messaging;

import java.util.Objects;

public class RapPluginErrorResponse extends RapPluginResponse {
    private String message;
    
    public RapPluginErrorResponse() {
        setResponseCode(500);
    }
    
    public RapPluginErrorResponse(int responseCode, String message) {
        this.message = message;
        setResponseCode(responseCode);
    }
    
    @Override
    public void setResponseCode(int responseCode) {
        if(responseCode >= 200 && responseCode < 300)
            throw new IllegalArgumentException("Response code should not be in range from 200-299.");
        super.setResponseCode(responseCode);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(message) + super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof RapPluginErrorResponse))
            return false;
        RapPluginErrorResponse other = (RapPluginErrorResponse) obj;
        
        return super.equals(other) && Objects.equals(message, other.message);
    }

    @Override
    public String getContent() {
        return "\"" + message.replace("\"", "\\\"") + "\"";
    }
}
