package eu.h2020.symbiote.rapplugin.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.data.InputParameter;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.enabler.messaging.model.rap.db.ResourceInfo;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Capability;
import eu.h2020.symbiote.model.cim.ComplexDatatype;
import eu.h2020.symbiote.model.cim.Datatype;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.semantics.ModelHelper;
import eu.h2020.symbiote.semantics.ontology.BIM;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;

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

    public static boolean isPrimitiveDatatype(Datatype datatype) {
        return PrimitiveDatatype.class.isAssignableFrom(datatype.getClass());
    }

    public static boolean isComplexDatatype(Datatype datatype) {
        return ComplexDatatype.class.isAssignableFrom(datatype.getClass());
    }

    public static <T> List<T> toList(final Iterator<T> iterator) {
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static <K, V> Map<K, V> toMap(final Iterator<Map.Entry<K, V>> iterator) {
        Map<K, V> result = new HashMap<>();
        if (iterator != null) {
            iterator.forEachRemaining(x -> result.put(x.getKey(), x.getValue()));
        }
        return result;
    }

    public static String UrlEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, "UTF-8").replaceAll("\\+", "%20")
                .replaceAll("\\%21", "!")
                .replaceAll("\\%27", "'")
                .replaceAll("\\%28", "(")
                .replaceAll("\\%29", ")")
                .replaceAll("\\%7E", "~")
                .replaceAll("\\%24", "\\$")
                .replaceAll("\\%3D", "=");
    }

    public static List<Literal> getPropertiesForClassFromPIM(String pimFile, String classUri, String propertyUri) {
        List<Literal> result = new ArrayList<>();
        try {
            OntModel model = null;
            if (pimFile == null || pimFile.isEmpty()) {
                // load BIM
                model = ModelHelper.readModel(BIM.getURI());
            } else {
                // load from file
                model = ModelHelper.readModel(pimFile, true, true);
            }
            result = ModelHelper.executeSelectAsLiteralList(model,
                    "SELECT \n"
                    + "DISTINCT ?o \n"
                    + "WHERE {\n"
                    + "    ?s a <" + classUri + "> .\n"
                    + "    ?s <" + propertyUri + "> ?o .\n"
                    + "    FILTER(isLiteral(?o)) \n"
                    + "}");
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public static Resource getResourceDescription(String interworkingInterfaceUrl, String internalId) throws UnsupportedEncodingException, IOException {
        return getResourceDescription(null, interworkingInterfaceUrl, internalId);
    }
    

    public static Resource getResourceDescription(HttpClient httpClient, String interworkingInterfaceUrl, String internalId) throws UnsupportedEncodingException, IOException {
        if (httpClient == null) {
            HttpClients.createDefault();
        }
        String encodedUrl = interworkingInterfaceUrl + "/rh/resource?" + Utils.UrlEncode("resourceInternalId=" + internalId);
        HttpResponse response = httpClient.execute(new HttpGet(encodedUrl));
        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException(response.getStatusLine().toString());
        }
        String json = EntityUtils.toString(response.getEntity());
        return new ObjectMapper().readValue(json, CloudResource.class).getResource();
    }

    public static List<Parameter> getServiceParameterDefinition(String interworkingInterfaceUrl, String internalId) throws UnsupportedEncodingException, IOException {
        return getServiceParameterDefinition(null, interworkingInterfaceUrl, internalId);
    }

    public static List<Parameter> getServiceParameterDefinition(HttpClient httpClient, String interworkingInterfaceUrl, String internalId) throws UnsupportedEncodingException, IOException {
        Resource resource = getResourceDescription(httpClient, interworkingInterfaceUrl, internalId);
        if (!Service.class.isAssignableFrom(resource.getClass())) {
            // TODO make more generic to handle actuators as well
            throw new RuntimeException("resource is not a Service");
        }
        return ((Service) resource).getParameters();        
    }

    public static List<Capability> getActuatorCapabilitiesParameterDefinition(String interworkingInterfaceUrl, String internalId) throws UnsupportedEncodingException, IOException {
        return getActuatorCapabilitiesParameterDefinition(null, interworkingInterfaceUrl, internalId);
    }
    
    public static List<Capability> getActuatorCapabilitiesParameterDefinition(HttpClient httpClient, String interworkingInterfaceUrl, String internalId) throws UnsupportedEncodingException, IOException {
        Resource resource = getResourceDescription(httpClient, interworkingInterfaceUrl, internalId);
        if (!Actuator.class.isAssignableFrom(resource.getClass())) {
            throw new RuntimeException("resource is not a Service");
        }
        return ((Actuator) resource).getCapabilities();
    }
}
