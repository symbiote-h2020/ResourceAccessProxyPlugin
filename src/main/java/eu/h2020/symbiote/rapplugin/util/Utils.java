package eu.h2020.symbiote.rapplugin.util;

import eu.h2020.symbiote.enabler.messaging.model.rap.db.ResourceInfo;
import java.util.List;

/**
 *
 * @author Michael Jacoby <michael.jacoby@iosb.fraunhofer.de>
 */
public class Utils {

    private Utils() {
    }

    /**
     * Check's is list of ResourceInfo identifies a single Resource.
     *
     * @param resourceInfo List of ResourceInfo to check
     * @return true if last element identifies a single resource with internal
     * ID
     */
    public static boolean isResourcePath(List<ResourceInfo> resourceInfo) {
        return resourceInfo != null
                && !resourceInfo.isEmpty()
                && resourceInfo.get(resourceInfo.size() - 1) != null
                && resourceInfo.get(resourceInfo.size() - 1).getInternalId() != null
                && !resourceInfo.get(resourceInfo.size() - 1).getInternalId().isEmpty();
    }

    /**
     * Returns a single internal resource ID for a list of ResourceInfo objects
     * if possible, null otherweise. For null-safe access, check with
     * isResourcePath before calling.
     *
     * @param resourceInfo List of ResourceInfo
     * @return The itnernal ID of a resource, if resourceInfo addresses a single
     * resource; otherwise null
     */
    public static String getInternalResourceId(List<ResourceInfo> resourceInfo) {
        if (isResourcePath(resourceInfo)) {
            return getLastResourceInfo(resourceInfo).getInternalId();
        }
        return null;
    }

    /**
     * Returns last resourceInfo from a list of ResourceInfo objects if
     * possible, null otherweise.
     *
     * @param resourceInfo List of ResourceInfo
     * @return Last elements of list is there is any, null otherweise
     */
    public static ResourceInfo getLastResourceInfo(List<ResourceInfo> resourceInfo) {
        if (resourceInfo == null || resourceInfo.isEmpty()) {
            return null;
        }
        return resourceInfo.get(resourceInfo.size() - 1);
    }
}
