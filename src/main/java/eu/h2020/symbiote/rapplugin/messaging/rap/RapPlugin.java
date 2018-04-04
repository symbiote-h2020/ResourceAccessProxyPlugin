/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.h2020.symbiote.cloud.model.data.Result;
import eu.h2020.symbiote.cloud.model.data.InputParameter;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.db.ResourceInfo;
import eu.h2020.symbiote.enabler.messaging.model.rap.registration.RegisterPluginMessage;
import eu.h2020.symbiote.enabler.messaging.model.rap.registration.UnregisterPluginMessage;
import eu.h2020.symbiote.rapplugin.domain.Capability;
import eu.h2020.symbiote.rapplugin.domain.Parameter;
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginErrorResponse;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginOkResponse;
import eu.h2020.symbiote.rapplugin.messaging.RapPluginResponse;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;
import eu.h2020.symbiote.model.cim.Observation;
import lombok.Getter;

/**
 * This is Spring component that handles requests from RAP. You can register different
 * listeners in this component. If you need reference to this component just autowire it.
 * 
 * @author Matteo Pardi <m.pardi@nextworks.it>
 * @author Mario Kušek <mario.kusek@fer.hr>
 * 
 */
public class RapPlugin implements SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(RapPlugin.class);

    private RabbitManager rabbitManager;

    private boolean running = false;

    @Getter
    private String enablerName;

    private boolean filtersSupported;

    private boolean notificationsSupported;

    private ReadingResourceListener readingResourceListener;

    private ActuatingResourceListener actuatingResourceListener;

    private NotificationResourceListener notificationResourceListener;
    
    private InvokingServiceListener invokingServiceListener;
    
    private ObjectMapper mapper;
    
    @Autowired
    public RapPlugin(RabbitManager rabbitManager, RapPluginProperties props) {
        this(rabbitManager, 
                props.getPluginName(), 
                props.getPlugin().isFiltersSupported(), 
                props.getPlugin().isNotificationsSupported()
        );
    }  
    
    public RapPlugin(RabbitManager rabbitManager, String enablerName, boolean filtersSupported,
                boolean notificationsSupported) {
        this.rabbitManager = rabbitManager;
        this.enablerName = enablerName;
        this.filtersSupported = filtersSupported;
        this.notificationsSupported = notificationsSupported;
        
        mapper = new ObjectMapper();
    }

    /**
     * Called when RabbitMQ message for reading resource arrives. 
     * @param msg AMQP message
     * @return response of reading resource
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "true", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.get'}"
    ))
    public RapPluginResponse fromAmqpReadResource(Message<?> msg) {
        LOG.debug("reading resource: {}", msg.getPayload());

        try {
            ResourceAccessGetMessage msgGet = deserializeRequest(msg, ResourceAccessGetMessage.class);
            
            List<ResourceInfo> resInfoList = msgGet.getResourceInfo();
            String internalId = null;
            for(ResourceInfo resInfo: resInfoList){
                String internalIdTemp = resInfo.getInternalId();
                if(internalIdTemp != null && !internalIdTemp.isEmpty())
                    internalId = internalIdTemp;
            }
            Observation observation = doReadResource(internalId);
            return new RapPluginOkResponse(observation);
        } catch (RapPluginException e) {
            return e.getResponse();
        } catch (Exception e) {
            if(msg.getPayload() instanceof byte[]) {
                String responseMsg = "Can not read Observation for request: " + new String((byte[])msg.getPayload(), StandardCharsets.UTF_8);
                LOG.error(responseMsg, e);
                return new RapPluginErrorResponse(500, responseMsg + "\n" + e.getMessage()); 
            } else {
                String responseMsg = "Can not read Observation for request: " + msg.getPayload();
                LOG.error(responseMsg, e);
                return new RapPluginErrorResponse(500, responseMsg + "\n" + e.getMessage());
            }
        }
    }

    /**
     * Called when RabbitMQ message for reading resource history arrives.
     * @param msg AMQP message
     * @return response of reading history
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "true", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.history'}"
            ))
    public RapPluginResponse fromAmqpHistoryResource(Message<?> msg) {
        LOG.debug("reading history resource: {}", msg.getPayload());
        
        try {
            ResourceAccessHistoryMessage msgHistory = deserializeRequest(msg, ResourceAccessHistoryMessage.class);
            
            List<ResourceInfo> resInfoList = msgHistory.getResourceInfo();
            String internalId = null;
            for(ResourceInfo resInfo: resInfoList){
                String internalIdTemp = resInfo.getInternalId();
                if(internalIdTemp != null && !internalIdTemp.isEmpty())
                    internalId = internalIdTemp;
            }
            List<Observation> observationList = doReadResourceHistory(internalId);
            return new RapPluginOkResponse(observationList);
        } catch (RapPluginException e) {
            return e.getResponse();
        } catch (Exception e) {
            if(msg.getPayload() instanceof byte[]) {
                String errorMsg = "Can not read history Observation for request: " + new String((byte[])msg.getPayload(), StandardCharsets.UTF_8);
                LOG.error(errorMsg, e);
                return new RapPluginErrorResponse(500, errorMsg + "\n" + e.getMessage());
            } else {
                String errorMsg = "Can not read history Observation for request: " + msg.getPayload();
                LOG.error(errorMsg, e);
                return new RapPluginErrorResponse(500, errorMsg + "\n" + e.getMessage());
            }
        }
    }
    
    /**
     * Called when RabbitMQ message for actuating or invoking service request arrives.
     * @param msg request with parameters
     * @return response of actuating or invoking service
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "true", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.set'}"
            ))
    public RapPluginResponse fromAmqpSetResource(Message<?> msg) {
        LOG.debug("actuating resource: {}", msg.getPayload());
        
        try {
            ResourceAccessSetMessage msgSet = deserializeRequest(msg, ResourceAccessSetMessage.class);
            
            List<ResourceInfo> resInfoList = msgSet.getResourceInfo();
            String internalId = null;
            String type = null;
            for(ResourceInfo resInfo: resInfoList){
                String internalIdTemp = resInfo.getInternalId();
                if(internalIdTemp != null && !internalIdTemp.isEmpty()) {
                    internalId = internalIdTemp;
                    type = resInfo.getType();
                }
            }
            
            if("Actuator".equals(type)) {
                Map<String,Capability> parameters = extractCapabilities(msgSet);
                doActuateResource(internalId, parameters);
                return new RapPluginOkResponse();
            } else {
                // service
                ArrayList<HashMap<String, Object>> jsonObject = 
                        mapper.readValue(msgSet.getBody(), new TypeReference<ArrayList<HashMap<String, Object>>>() { });
                Map<String, Parameter> parameters = new HashMap<>();
                for(HashMap<String,Object> parametersMap: jsonObject) {
                    for(Entry<String, Object> parameter: parametersMap.entrySet()) {
                        Parameter p = new Parameter(parameter.getKey(), parameter.getValue());
                        parameters.put(p.getName(), p);
                    }
                }
                Object result = doInvokeService(internalId, parameters);
                if(result == null)
                    return new RapPluginOkResponse();
                else
                    return new RapPluginOkResponse(result);
                
            }
        } catch (RapPluginException e) {
            return e.getResponse();
        } catch (Exception e) {
            if(msg.getPayload() instanceof byte[]) {
                String responseMsg = "Can not set/call service for request: " + new String((byte[])msg.getPayload(), StandardCharsets.UTF_8);
                LOG.error(responseMsg, e);
                return new RapPluginErrorResponse(500, responseMsg + "\n" + e.getMessage()); 
            } else {
                String responseMsg = "Can not set/call service for request: " + msg.getPayload();
                LOG.error(responseMsg, e);
                return new RapPluginErrorResponse(500, responseMsg + "\n" + e.getMessage());
            }
        }
    }

    private Map<String,Capability> extractCapabilities(ResourceAccessSetMessage msgSet)
            throws IOException, JsonParseException, JsonMappingException {
        Map<String,Capability> capabilitiesList = new HashMap<>();
        
        HashMap<String,ArrayList<HashMap<String, Object>>> capabilityMap = 
                mapper.readValue(msgSet.getBody(), new TypeReference<HashMap<String,ArrayList<HashMap<String, Object>>>>() { });
        for(Entry<String, ArrayList<HashMap<String,Object>>> capabilityEntry: capabilityMap.entrySet()) {
            LOG.debug("Found capability {}", capabilityEntry.getKey());
            LOG.debug(" There are {} parameters.", capabilityEntry.getValue().size());
            Capability capability = new Capability(capabilityEntry.getKey());
            capabilitiesList.put(capability.getName(), capability);
            for(HashMap<String, Object> parameterMap: capabilityEntry.getValue()) {
                for(Entry<String, Object> parameter: parameterMap.entrySet()) {
                    LOG.debug(" paramName: {}", parameter.getKey());
                    LOG.debug(" paramValueType: {} value: {}\n", parameter.getValue().getClass().getName(), parameter.getValue());
                    Parameter inputParameter = new Parameter(parameter.getKey(), parameter.getValue());
                    capability.addParameter(inputParameter);
                }
            }
        }
        
        return capabilitiesList;
    }


    private <O> O deserializeRequest(Message<?> msg, Class<O> clazz)
            throws IOException, JsonParseException, JsonMappingException {
        String stringMsg;
        if(msg.getPayload() instanceof byte[])
            stringMsg = new String((byte[]) msg.getPayload(), StandardCharsets.UTF_8);
        else if (msg.getPayload() instanceof String)
            stringMsg = (String) msg.getPayload();
        else
            throw new RuntimeException("Can not cast payload to byte[] or string. Payload is of type " + 
                    msg.getPayload().getClass().getName() + ". Payload: " + msg.getPayload());
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
            switch(access) {
                // TODO test SUBSCRIBE
                case SUBSCRIBE: {
                    // WebSocketController
                    ResourceAccessSubscribeMessage mess = (ResourceAccessSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
                        doSubscribeResource(info.getInternalId());
                    }
                    break;
                }
                // TODO test unsubscribe
                // TODO test notifications sending
                case UNSUBSCRIBE: {
                    // WebSocketController
                    ResourceAccessUnSubscribeMessage mess = (ResourceAccessUnSubscribeMessage)msg;
                    List<ResourceInfo> infoList = mess.getResourceInfoList();
                    for(ResourceInfo info : infoList) {
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
        } catch (Exception e ) {
            LOG.error("Error while registering plugin for platform " + platformId + "\n" + e);
        }
    }
    
    private void unregisterPlugin(String platformId) {
        try {
            UnregisterPluginMessage msg = new UnregisterPluginMessage(platformId);

            rabbitManager.sendMessage(RapDefinitions.PLUGIN_REGISTRATION_EXCHANGE_OUT, 
                    RapDefinitions.PLUGIN_REGISTRATION_KEY, msg);
        } catch (Exception e ) {
            LOG.error("Error while unregistering plugin for platform " + platformId + "\n" + e);
        }
    }
    
    /**
     * Registers listener for reading resource.
     * 
     * @param listener
     */
    public void registerReadingResourceListener(ReadingResourceListener listener) {
        this.readingResourceListener = listener;
    }

    /**
     * Unregisters listener for reading resource.
     * 
     * @param listener
     */
    public void unregisterReadingResourceListener(ReadingResourceListener listener) {
        this.readingResourceListener = null;
    }

    /**
     * Execute reading resource. 
     * 
     * @param resourceId internal resource id
     * @return observation
     */
    public Observation doReadResource(String resourceId) {
        if(readingResourceListener == null)
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
                    
        return readingResourceListener.readResource(resourceId);
    }
    
    /**
     * Executes reading history of observations. Max number of observations is 100.
     * 
     * @param resourceId internal resource id
     * @return list of observations (max. 100 values)
     */
    public List<Observation> doReadResourceHistory(String resourceId) {
        if(readingResourceListener == null)
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
                    
        return readingResourceListener.readResourceHistory(resourceId);
    }
    
    /**
     * Registers listener for actuating resource.
     * 
     * @param listener
     */
    public void registerActuatingResourceListener(ActuatingResourceListener listener) {
        this.actuatingResourceListener = listener;
    }

    /**
     * Unregisters listener for actuating resource.
     * 
     * @param listener
     */
    public void unregisterActuatingResourceListener(ActuatingResourceListener listener) {
        this.actuatingResourceListener = null;
    }

    /**
     * Executes actuation of resources.
     * 
     * @param resourceId internal resource id
     * @param capabilities map of capabilities. Key is capability name and value is capability 
     * object with parameters.
     */
    public void doActuateResource(String resourceId, Map<String,Capability> capabilities) {
        if(actuatingResourceListener == null)
            throw new RuntimeException("ActuatingResourceListener not registered in RapPlugin");
                    
        actuatingResourceListener.actuateResource(resourceId, capabilities);
    }
    
    /**
     * Registers listener for invonking service.
     * 
     * @param invokingServiceListener
     */
    public void registerInvokingServiceListener(InvokingServiceListener invokingServiceListener) {
        this.invokingServiceListener = invokingServiceListener;
    }

    /**
     * Unregisters listener for invoking service.
     * 
     * @param invokingServiceListener
     */
    public void unregisterInvokingServiceListener(InvokingServiceListener invokingServiceListener) {
        this.invokingServiceListener = null;       
    }
    
    /**
     * Executes invoking service.
     * 
     * @param internalId internal resource id
     * @param parameters map of parameters. Key is paremeter name and value is parameter.
     * @return result of invoking service
     */
    public Object doInvokeService(String internalId, Map<String, Parameter> parameters) {
        if(invokingServiceListener == null)
            throw new RuntimeException("InvokingServiceListener not registered in RapPlugin");
                    
        return invokingServiceListener.invokeService(internalId, parameters);
    }

    /**
     * Registers listener for notification when resource observation is changed.
     * 
     * @param listener
     */
    public void registerNotificationResourceListener(NotificationResourceListener listener) {
        this.notificationResourceListener = listener;
    }

    /**
     * Unregisters notification listener.
     * 
     * @param listener
     */
    public void unregisterNotificationResourceListener(NotificationResourceListener listener) {
        this.notificationResourceListener = null;
    }

    /**
     * Initiate client subscription for notification of some resource observation change.
     * 
     * @param resourceId internal resource id
     */
    public void doSubscribeResource(String resourceId) {
        if(notificationResourceListener == null)
            throw new RuntimeException("NotificationResourceListener not registered in RapPlugin");
                    
        notificationResourceListener.subscribeResource(resourceId);
    }
    
    /**
     * Unsubscribe on notification of resource observation change.
     * 
     * @param resourceId internal resource id
     */
    public void doUnsubscribeResource(String resourceId) {
        if(notificationResourceListener == null)
            throw new RuntimeException("NotificationResourceListener not registered in RapPlugin");
                    
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
}
