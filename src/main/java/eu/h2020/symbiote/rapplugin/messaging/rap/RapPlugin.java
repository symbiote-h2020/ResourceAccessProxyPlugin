package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.cloud.model.rap.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.cloud.model.rap.ResourceInfo;
import eu.h2020.symbiote.cloud.model.rap.query.Query;
import eu.h2020.symbiote.cloud.model.rap.registration.RegisterPluginMessage;
import eu.h2020.symbiote.cloud.model.rap.registration.UnregisterPluginMessage;
import eu.h2020.symbiote.rapplugin.CapabilityDeserializer;
import eu.h2020.symbiote.rapplugin.DeserializerRegistry;
import eu.h2020.symbiote.rapplugin.ParameterDeserializer;
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginErrorResponse;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginOkResponse;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginResponse;
import eu.h2020.symbiote.rapplugin.properties.Properties;
import eu.h2020.symbiote.rapplugin.util.Utils;
import eu.h2020.symbiote.rapplugin.value.Value;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This is Spring component that handles requests from RAP. You can register
 * different listeners in this component. If you need reference to this
 * component just autowire it.
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 * @author Mario Kušek <mario.kusek@fer.hr>
 *
 */
@Component
public class RapPlugin implements SmartLifecycle {

    public static final String TYPE_ACTUATOR = "Actuator";
    public static final String TYPE_SERVICE = "Service";

    private static final Logger LOG = LoggerFactory.getLogger(RapPlugin.class);

    private RabbitManager rabbitManager;

    private boolean running = false;

    @Getter
    private String enablerName;

    private boolean filtersSupported;

    private boolean notificationsSupported;

    private ResourceAccessListener readingResourceListener;

    private ActuatorAccessListener actuatingResourceListener;

    private SubscriptionListener notificationResourceListener;

    private ServiceAccessListener invokingServiceListener;

    private ObjectMapper mapper;

    private DeserializerRegistry deserializerRegistry;

    private final String registrationHandlerUrl;

    @Autowired
    public RapPlugin(RabbitManager rabbitManager, Properties props) {
        this(rabbitManager,
                props.getPluginName(),
                props.getPlugin().isFiltersSupported(),
                props.getPlugin().isNotificationsSupported(),
                props.getPlugin().getRegistrationHandlerUrl()
        );
    }

    public RapPlugin(
            RabbitManager rabbitManager, 
            String enablerName, 
            boolean filtersSupported,
            boolean notificationsSupported,
            String registrationHandlerUrl) {
        this.rabbitManager = rabbitManager;
        this.enablerName = enablerName;
        this.filtersSupported = filtersSupported;
        this.notificationsSupported = notificationsSupported;
        this.deserializerRegistry = new DeserializerRegistry();
        this.registrationHandlerUrl = registrationHandlerUrl;
        mapper = new ObjectMapper();
    }

    /**
     * Called when RabbitMQ message for reading resource arrives.
     *
     * @param msg AMQP message
     * @return response of reading resource
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "false", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.get'}"
    ))
    public RapPluginResponse fromAmqpReadResource(Message<?> msg) {
        LOG.debug("reading resource: {}", msg.getPayload());

        try {
            ResourceAccessGetMessage message = deserializeRequest(msg, ResourceAccessGetMessage.class);
            String result = doReadResource(message.getResourceInfo());
            return new RapPluginOkResponse(result);
        } catch (RapPluginException e) {
            LOG.warn(generateErrorResponseMessage("There is error in plugin when reading resource. RabbitMQ message: ", msg), e);
            return e.getResponse();
        } catch (Exception e) {
            String responseMsg = generateErrorResponseMessage("Can not read Observation for request: ", msg);
            LOG.error(responseMsg, e);
            return new RapPluginErrorResponse(500, responseMsg + "\n" + e.getMessage());
        }
    }

    private String generateErrorResponseMessage(String errorMsg, Message<?> msg) {
        String responseMsg;
        if (msg.getPayload() instanceof byte[]) {
            responseMsg = errorMsg + new String((byte[]) msg.getPayload(), StandardCharsets.UTF_8);
        } else {
            responseMsg = errorMsg + msg.getPayload();
        }
        return responseMsg;
    }

    /**
     * Called when RabbitMQ message for reading resource history arrives.
     *
     * @param msg AMQP message
     * @return response of reading history
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "false", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.history'}"
    ))
    public RapPluginResponse fromAmqpHistoryResource(Message<?> msg) {
        LOG.debug("reading history resource: {}", msg.getPayload());

        try {
            ResourceAccessHistoryMessage msgHistory = deserializeRequest(msg, ResourceAccessHistoryMessage.class);
            String result = doReadResourceHistory(msgHistory.getResourceInfo(), msgHistory.getTop(), msgHistory.getFilter());
            return new RapPluginOkResponse(result);
        } catch (RapPluginException e) {
            LOG.warn(generateErrorResponseMessage("There is error in plugin when reading history of resource. RabbitMQ message: ", msg), e);
            return e.getResponse();
        } catch (Exception e) {
            String errorMsg = generateErrorResponseMessage("Can not read history Observation for request: ", msg);
            LOG.error(errorMsg, e);
            return new RapPluginErrorResponse(500, errorMsg + "\n" + e.getMessage());
        }
    }

    /**
     * Called when RabbitMQ message for actuating or invoking service request
     * arrives.
     *
     * @param msg request with parameters
     * @return response of actuating or invoking service
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "false", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.set'}"
    ))
    public RapPluginResponse fromAmqpSetResource(Message<?> msg) {
        LOG.debug("actuating resource: {}", msg.getPayload());

        try {
            ResourceAccessSetMessage message = deserializeRequest(msg, ResourceAccessSetMessage.class);
            List<ResourceInfo> resourceInfo = message.getResourceInfo();
            if (!Utils.isResourcePath(resourceInfo)) {
                throw new RapPluginException(HttpStatus.NOT_IMPLEMENTED.value(), "actuation/service invocation only allowed on uniquely indentifiable resource");
            }
            ResourceInfo lastResourceInfo = Utils.getLastResourceInfo(resourceInfo);
            String internalId = lastResourceInfo.getInternalId();
            if(message.getBody().trim().startsWith("{")) { 
            //if (TYPE_ACTUATOR.equalsIgnoreCase(lastResourceInfo.getType())) {
                // actuation
                doActuateResource(internalId,
                        CapabilityDeserializer.deserialize(registrationHandlerUrl, 
                                getDeserializerRegistry(),
                                internalId,
                                message.getBody()));
                return new RapPluginOkResponse();
            } else if (message.getBody().trim().startsWith("[")) {
            //} else if (TYPE_SERVICE.equalsIgnoreCase(lastResourceInfo.getType())) {
                return new RapPluginOkResponse(doInvokeService(internalId,
                        ParameterDeserializer.deserialize(registrationHandlerUrl, 
                                getDeserializerRegistry(),
                                internalId,
                                message.getBody())));
            } else {
                throw new RapPluginException(HttpStatus.BAD_REQUEST.value(), "SET not allowed on resource type '" + lastResourceInfo.getType() + "'");
            }

        } catch (RapPluginException e) {
            LOG.warn(generateErrorResponseMessage("There is error in plugin in actuation/service invocation. RabbitMQ message: ", msg), e);
            return e.getResponse();
        } catch (Exception e) {
            String responseMsg = generateErrorResponseMessage("Can not set/call service for request: ", msg);
            LOG.error(responseMsg, e);
            return new RapPluginErrorResponse(500, responseMsg + "\n" + e.getMessage());
        }
    }

//    private Map<String, Value> extractParameters(ResourceAccessSetMessage message) throws IOException {
//        return parameterDeserializer.deserialize(Utils.getInternalResourceId(message.getResourceInfo()), message.getBody());
//    }
//
//    private Map<String, Map<String, Value>> extractCapabilities(ResourceAccessSetMessage message)
//            throws IOException, JsonParseException, JsonMappingException {
//        String jsonParameter = message.getBody();
//        String internalId = Utils.getInternalResourceId(message.getResourceInfo());
//
//    }
    private <O> O deserializeRequest(Message<?> msg, Class<O> clazz)
            throws IOException, JsonParseException, JsonMappingException {
        String stringMsg;
        if (msg.getPayload() instanceof byte[]) {
            stringMsg = new String((byte[]) msg.getPayload(), StandardCharsets.UTF_8);
        } else if (msg.getPayload() instanceof String) {
            stringMsg = (String) msg.getPayload();
        } else {
            throw new RuntimeException("Can not cast payload to byte[] or string. Payload is of type "
                    + msg.getPayload().getClass().getName() + ". Payload: " + msg.getPayload());
        }
        O obj = mapper.readValue(stringMsg, clazz);
        return obj;
    }

    // TODO removed eventualy (when all feature will be implemented) because it is not used 
    // class which sends this is ResourceAccessRestController
    public String receiveMessage(String message) {
        String json = null;
        try {
            ResourceAccessMessage msg = mapper.readValue(message, ResourceAccessMessage.class);
            ResourceAccessMessage.AccessType access = msg.getAccessType();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            switch (access) {
                // TODO test SUBSCRIBE
                case SUBSCRIBE: {
                    // WebSocketController
                    ResourceAccessSubscribeMessage mess = (ResourceAccessSubscribeMessage) msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for (ResourceInfo info : infoList) {
                        doSubscribeResource(info.getInternalId());
                    }
                    break;
                }
                // TODO test unsubscribe
                // TODO test notifications sending
                case UNSUBSCRIBE: {
                    // WebSocketController
                    ResourceAccessUnSubscribeMessage mess = (ResourceAccessUnSubscribeMessage) msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for (ResourceInfo info : infoList) {
                        doUnsubscribeResource(info.getInternalId());
                    }
                    break;
                }
                default:
                    throw new Exception("Access type " + access.toString() + " not supported");
            }
        } catch (Exception e) {
            LOG.error("Error while processing message:\n" + message + "\n" + e);
        }
        return json;
    }

    private void registerPlugin(String platformId, boolean hasFilters, boolean hasNotifications) {
        try {
            RegisterPluginMessage msg = new RegisterPluginMessage(platformId, hasFilters, hasNotifications);

            rabbitManager.sendMessage(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_OUT,
                    RapDefinitions.PLUGIN_REGISTRATION_KEY, msg);
        } catch (Exception e) {
            LOG.error("Error while registering plugin for platform " + platformId + "\n" + e);
        }
    }

    private void unregisterPlugin(String platformId) {
        try {
            UnregisterPluginMessage msg = new UnregisterPluginMessage(platformId);

            rabbitManager.sendMessage(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_OUT,
                    RapDefinitions.PLUGIN_REGISTRATION_KEY, msg);
        } catch (Exception e) {
            LOG.error("Error while unregistering plugin for platform " + platformId + "\n" + e);
        }
    }

    /**
     * Registers listener for reading resource.
     *
     * @param listener
     */
    public void registerReadingResourceListener(ResourceAccessListener listener) {
        this.readingResourceListener = listener;
    }

    /**
     * Unregisters listener for reading resource.
     *
     * @param listener
     */
    public void unregisterReadingResourceListener(ResourceAccessListener listener) {
        this.readingResourceListener = null;
    }

    /**
     * Execute reading resource.
     *
     * @param resourceInfo info to identify requested resource
     * @return observation
     */
    public String doReadResource(List<ResourceInfo> resourceInfo) {
        if (readingResourceListener == null) {
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
        }

        return readingResourceListener.getResource(resourceInfo);
    }

    /**
     * Executes reading history of observations. Max number of observations is
     * 100.
     *
     * @param resourceInfo info identifying the requested resource
     * @param top number specifying the top-k values to fetch, -1 to fetch all
     * @param filterQuery filter defined in the request, null if none defined
     * @return JSON array of resource history as String
     */
    public String doReadResourceHistory(List<ResourceInfo> resourceInfo, int top, Query filterQuery) {
        if (readingResourceListener == null) {
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
        }

        return readingResourceListener.getResourceHistory(resourceInfo, top, filterQuery);
    }

    /**
     * Registers listener for actuating resource.
     *
     * @param listener
     */
    public void registerActuatingResourceListener(ActuatorAccessListener listener) {
        this.actuatingResourceListener = listener;
    }

    /**
     * Unregisters listener for actuating resource.
     *
     * @param listener
     */
    public void unregisterActuatingResourceListener(ActuatorAccessListener listener) {
        this.actuatingResourceListener = null;
    }

    /**
     * Executes actuation of resources.
     *
     * @param internalId internal resource id
     * @param capabilities map of capabilities. Key is capability name and value
     * is capability object with parameters.
     */
    public void doActuateResource(String internalId, Map<String, Map<String, Value>> capabilities) {
        if (actuatingResourceListener == null) {
            throw new RuntimeException("ActuatingResourceListener not registered in RapPlugin");
        }

        actuatingResourceListener.actuateResource(internalId, capabilities);
    }

    /**
     * Registers listener for invonking service.
     *
     * @param invokingServiceListener
     */
    public void registerInvokingServiceListener(ServiceAccessListener invokingServiceListener) {
        this.invokingServiceListener = invokingServiceListener;
    }

    /**
     * Unregisters listener for invoking service.
     *
     * @param invokingServiceListener
     */
    public void unregisterInvokingServiceListener(ServiceAccessListener invokingServiceListener) {
        this.invokingServiceListener = null;
    }

    /**
     * Executes invoking service.
     *
     * @param internalId internal resource id
     * @param parameters map of parameters. Key is paremeter name and value is
     * parameter.
     * @return result of invoking service
     */
    public String doInvokeService(String internalId, Map<String, Value> parameters) {
        if (invokingServiceListener == null) {
            throw new RuntimeException("InvokingServiceListener not registered in RapPlugin");
        }

        return invokingServiceListener.invokeService(internalId, parameters);
    }

    /**
     * Registers listener for notification when resource observation is changed.
     *
     * @param listener
     */
    public void registerNotificationResourceListener(SubscriptionListener listener) {
        this.notificationResourceListener = listener;
    }

    /**
     * Unregisters notification listener.
     *
     * @param listener
     */
    public void unregisterNotificationResourceListener(SubscriptionListener listener) {
        this.notificationResourceListener = null;
    }

    /**
     * Initiate client subscription for notification of some resource
     * observation change.
     *
     * @param resourceId internal resource id
     */
    public void doSubscribeResource(String resourceId) {
        if (notificationResourceListener == null) {
            throw new RuntimeException("NotificationResourceListener not registered in RapPlugin");
        }

        notificationResourceListener.subscribeResource(resourceId);
    }

    /**
     * Unsubscribe on notification of resource observation change.
     *
     * @param resourceId internal resource id
     */
    public void doUnsubscribeResource(String resourceId) {
        if (notificationResourceListener == null) {
            throw new RuntimeException("NotificationResourceListener not registered in RapPlugin");
        }

        notificationResourceListener.unsubscribeResource(resourceId);
    }

    // Spring SmartLifecycle methods
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        running = false;
        callback.run();
    }

    @Override
    public void start() {
        registerPlugin(enablerName, filtersSupported, notificationsSupported);
    }

    @Override
    public void stop() {
        unregisterPlugin(enablerName);
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }

    public DeserializerRegistry getDeserializerRegistry() {
        return deserializerRegistry;
    }

    public void setDeserializerRegistry(DeserializerRegistry deserializerRegistry) {
        this.deserializerRegistry = deserializerRegistry;
    }
}
