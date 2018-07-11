package eu.h2020.symbiote.rapplugin.messaging.rap;

import eu.h2020.symbiote.enabler.messaging.model.rap.db.ResourceInfo;
import eu.h2020.symbiote.enabler.messaging.model.rap.query.Query;
import java.util.List;

/**
 * Listener for reading resources.
 *
 *
 * @author Mario Kušek <mario.kusek@fer.hr>
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 *
 */
public interface ResourceAccessListener {

    /**
     * This method is called when RAP is asking for resource data. In
     * implementation you should put the query to the platform with internal
     * resourceId to get data.
     *
     * @param resourceInfo info identifying the requested resource
     * @return resource as JSON
     */
    public String getResource(List<ResourceInfo> resourceInfo);

    /**
     * This method is called when DSI/RAP is asking for historical resource
     * data. In implementation you should put the query to the platform with
     * internal resource id to get historical data.
     *
     * @param resourceInfo info identifying the requested resource
     * @param top number specifying the top-k values to fetch, -1 to fetch all
     * @param filterQuery filter defined in the request, null if none defined
     * @return JSON array of resource history as String
     */
    public String getResourceHistory(List<ResourceInfo> resourceInfo, int top, Query filterQuery);
}
