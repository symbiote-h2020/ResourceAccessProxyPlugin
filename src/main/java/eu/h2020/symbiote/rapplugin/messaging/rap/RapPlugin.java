/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.messaging.rap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

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
import eu.h2020.symbiote.rapplugin.messaging.RabbitManager;
import eu.h2020.symbiote.rapplugin.properties.RapPluginProperties;
import eu.h2020.symbiote.model.cim.Observation;
import lombok.Getter;

/**
 * This is class that handles requests from RAP.
 * 
 * @author Matteo Pardi <m.pardi@nextworks.it>
 * @author Mario Kušek <mario.kusek@fer.hr>
 * 
 */
@Service
public class RapPlugin implements SmartLifecycle {
    private static final Logger LOG = LoggerFactory.getLogger(RapPlugin.class);

    private RabbitManager rabbitManager;

    private boolean running = false;

    @Getter
    private String enablerName;

    private boolean filtersSupported;

    private boolean notificationsSupported;

    private ReadingResourceListener readingResourceListener;

    private WritingToResourceListener writingToResourceListener;

    private NotificationResourceListener notificationResourceListener;
    
    @Autowired
    public RapPlugin(RabbitManager rabbitManager, RapPluginProperties props) {
        this(rabbitManager, 
                props.getEnablerName(), 
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
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "true", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.get'}"
    ))
    public List<Observation> fromAmqpReadResource(Message<?> msg) {
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
            List<Observation> observationList = doReadResource(internalId);
            return observationList;
        } catch (Exception e) {
            if(msg.getPayload() instanceof byte[])
                LOG.error("Can not read Observation for request: " + new String((byte[])msg.getPayload(), StandardCharsets.UTF_8), e);
            else
                LOG.error("Can not read Observation for request: " + msg.getPayload(), e);
        }

        return new LinkedList<>();
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "true", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.history'}"
            ))
    public List<Observation> fromAmqpHistoryResource(Message<?> msg) {
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
            return observationList;
        } catch (Exception e) {
            if(msg.getPayload() instanceof byte[])
                LOG.error("Can not read history Observation for request: " + new String((byte[])msg.getPayload(), StandardCharsets.UTF_8), e);
            else
                LOG.error("Can not read history Observation for request: " + msg.getPayload(), e);
        }
        
        return new LinkedList<>();
    }
    
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = "plugin-exchange", type = "topic", durable = "true", autoDelete = "false", ignoreDeclarationExceptions = "true"),
            key = "#{rapPlugin.enablerName + '.set'}"
            ))
    public Result<Object> fromAmqpSetResource(Message<?> msg) {
        LOG.debug("reading history resource: {}", msg.getPayload());
        
        try {
            ResourceAccessSetMessage msgSet = deserializeRequest(msg, ResourceAccessSetMessage.class);
            
            List<ResourceInfo> resInfoList = msgSet.getResourceInfo();
            String internalId = null;
            for(ResourceInfo resInfo: resInfoList){
                String internalIdTemp = resInfo.getInternalId();
                if(internalIdTemp != null && !internalIdTemp.isEmpty())
                    internalId = internalIdTemp;
            }
            
            List<InputParameter> parameters = new ObjectMapper().readValue(msgSet.getBody(), new TypeReference<List<InputParameter>>() { });
            Result<Object> result = doWriteResource(internalId, parameters);
            if(result.getValue() == null) return null;
            
            return result;
        } catch (Exception e) {
            if(msg.getPayload() instanceof byte[])
                LOG.error("Can not set/call service for request: " + new String((byte[])msg.getPayload(), StandardCharsets.UTF_8), e);
            else
                LOG.error("Can not set/call service for request: " + msg.getPayload(), e);
        }
        
        return null;
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
        ObjectMapper mapper = new ObjectMapper();
        O obj = mapper.readValue(stringMsg, clazz);
        return obj;
    }

    
    // class which sends this is ResourceAccessRestController
    public String receiveMessage(String message) {
        String json = null;
        try {            
            ObjectMapper mapper = new ObjectMapper();
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
    
    public void registerReadingResourceListener(ReadingResourceListener listener) {
        this.readingResourceListener = listener;
    }

    public void unregisterReadingResourceListener(ReadingResourceListener listener) {
        this.readingResourceListener = null;
    }

    public List<Observation> doReadResource(String resourceId) {
        if(readingResourceListener == null)
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
                    
        return readingResourceListener.readResource(resourceId);
    }
    
    public List<Observation> doReadResourceHistory(String resourceId) {
        if(readingResourceListener == null)
            throw new RuntimeException("ReadingResourceListener not registered in RapPlugin");
                    
        return readingResourceListener.readResourceHistory(resourceId);
    }
    
    public void registerWritingToResourceListener(WritingToResourceListener listener) {
        this.writingToResourceListener = listener;
    }

    public void unregisterWritingToResourceListener(WritingToResourceListener listener) {
        this.writingToResourceListener = null;
    }

    public Result<Object> doWriteResource(String resourceId, List<InputParameter> parameters) {
        if(writingToResourceListener == null)
            throw new RuntimeException("WritingToResourceListener not registered in RapPlugin");
                    
        return writingToResourceListener.writeResource(resourceId, parameters);
    }

    public void registerNotificationResourceListener(NotificationResourceListener listener) {
        this.notificationResourceListener = listener;
    }

    public void unregisterNotificationResourceListener(NotificationResourceListener listener) {
        this.notificationResourceListener = null;
    }

    public void doSubscribeResource(String resourceId) {
        if(notificationResourceListener == null)
            throw new RuntimeException("NotificationResourceListener not registered in RapPlugin");
                    
        notificationResourceListener.subscribeResource(resourceId);
    }
    
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
