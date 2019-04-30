package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.util.List;

import eu.h2020.symbiote.model.cim.Observation;

/**
 * Listener for reading resources. 
 * 
 * 
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 * @deprecated Replaced by {@link ResourceAccessListener}
 */
@Deprecated(forRemoval = true)
public interface ReadingResourceListener {
    /**  
     * This method is called when RAP is asking for resource data.
     * In implementation you should put the query to the platform with 
     * internal resourceId to get data.
     * 
     * @param resourceId internal resource id
     * @return observed value
     */
    Observation readResource(String resourceId);
    
    /**
     * This method is called when DSI/RAP is asking for historical resource data.
     * In implementation you should put the query to the platform with internal 
     * resource id to get historical data.
     * 
     * @param resourceId internal resource id
     * @return list of historical observed values
     */
    List<Observation> readResourceHistory(String resourceId);
}
