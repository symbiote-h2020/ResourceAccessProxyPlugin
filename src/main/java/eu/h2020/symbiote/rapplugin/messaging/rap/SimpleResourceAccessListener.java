package eu.h2020.symbiote.rapplugin.messaging.rap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.cloud.model.rap.query.Query;
import eu.h2020.symbiote.rapplugin.util.Utils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Helper class to ensure backwards-compatibility for resource access.
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public abstract class SimpleResourceAccessListener implements ResourceAccessListener {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleResourceAccessListener.class);

    /**
     * This method is called when RAP is asking for resource data. In
     * implementation you should put the query to the platform with internal
     * resourceId to get data.
     *
     * @param resourceId internal resource id
     * @return observed value
     */
    public abstract String readResource(String resourceId);

    /**
     * This method is called when DSI/RAP is asking for historical resource
     * data. In implementation you should put the query to the platform with
     * internal resource id to get historical data.
     *
     * @param resourceId internal resource id
     * @return list of historical observed values
     */
    public abstract String readResourceHistory(String resourceId);

    @Override
    public String getResource(List<ResourceInfo> resourceInfo) {
        if (Utils.isResourcePath(resourceInfo)) {
            String lastInternalId = Utils.getInternalResourceId(resourceInfo);
            try {
                return new ObjectMapper().writeValueAsString(readResource(lastInternalId));
            } catch (JsonProcessingException ex) {
                String message = "could not serialize resource to JSON";
                LOG.error(message, ex);
                throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
            }
        } else {
            throw new RapPluginException(HttpStatus.NOT_IMPLEMENTED.value(), "only access on resource level supported");
        }
    }

    @Override
    public String getResourceHistory(List<ResourceInfo> resourceInfo, int top, Query filterQuery) {
        if (Utils.isResourcePath(resourceInfo)) {
            if (filterQuery != null) {
                throw new RapPluginException(HttpStatus.NOT_IMPLEMENTED.value(), "filter operator not supported");
            }
            String lastInternalId = Utils.getInternalResourceId(resourceInfo);
            try {
                return new ObjectMapper().writeValueAsString(readResource(lastInternalId));
            } catch (JsonProcessingException ex) {
                String message = "could not serialize resource to JSON";
                LOG.error(message, ex);
                throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
            }
        } else {
            throw new RapPluginException(HttpStatus.NOT_IMPLEMENTED.value(), "only access on resource level supported");
        }
    }

}
