package eu.h2020.symbiote.rapplugin.messaging.rap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.cloud.model.rap.query.Query;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.rapplugin.util.Utils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * Helper class to ensure backwards-compatibility for resource access.
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 * @author Mario Kusek <mario.kusek@fer.hr>
 * 
 */
@Deprecated(forRemoval = true)
public class ResourceAccessListenerAdapter implements ResourceAccessListener {

    static final Logger LOG = LoggerFactory.getLogger(ResourceAccessListenerAdapter.class);
    
    private ReadingResourceListener delegate;
    
    public ResourceAccessListenerAdapter(ReadingResourceListener listener) {
        delegate = listener;
    }

    @Override
    public String getResource(List<ResourceInfo> resourceInfo) {
        if (Utils.isSensorPath(resourceInfo)) {
            String lastInternalId = Utils.getInternalResourceId(resourceInfo);
            Observation readResourceResult = delegate.readResource(lastInternalId);
            try {
                return new ObjectMapper().writeValueAsString(readResourceResult);
            } catch (JsonProcessingException ex) {
                String message = "Could not serialize resource returned from ReadingResourceListener.readResource of type " + 
                        readResourceResult.getClass().getCanonicalName() + " to JSON";
                throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, ex);
            }
        } else {
            throw new RapPluginException(HttpStatus.NOT_IMPLEMENTED.value(), "only access on resource level supported");
        }
    }

    @Override
    public String getResourceHistory(List<ResourceInfo> resourceInfo, int top, Query filterQuery) {
        if (Utils.isSensorPath(resourceInfo)) {
            if (filterQuery != null) {
                throw new RapPluginException(HttpStatus.NOT_IMPLEMENTED.value(), "filter operator not supported");
            }
            String lastInternalId = Utils.getInternalResourceId(resourceInfo);
            List<Observation> readResourceHistoryResult = delegate.readResourceHistory(lastInternalId);
            try {
                return new ObjectMapper().writeValueAsString(readResourceHistoryResult);
            } catch (JsonProcessingException ex) {
                String message = "Could not serialize resource returned from ReadingResourceListener.readResourceHistory of type " + 
                        readResourceHistoryResult.getClass().getCanonicalName() + " to JSON";
                throw new RapPluginException(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, ex);
            }
        } else {
            throw new RapPluginException(HttpStatus.NOT_IMPLEMENTED.value(), "only access on resource level supported");
        }
    }
}
