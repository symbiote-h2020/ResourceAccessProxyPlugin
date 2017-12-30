package eu.h2020.symbiote.rapplugin.messaging.rap;

public interface NotificationResourceListener {
    /**
     * This method is called when a client want to subscribe to specified resource. 
     * Implementation should start the subscription of the resource.
     * 
     * @param resourceId internal resource id
     */
    void subscribeResource(String resourceId);
    
    /**  
     * This method is called when a client want to unsubscribe from specified resource. 
     * Implementation should stop the subscription to the resource.
     * 
     * @param resourceId internal resource id
     */
    void unsubscribeResource(String resourceId);
}
