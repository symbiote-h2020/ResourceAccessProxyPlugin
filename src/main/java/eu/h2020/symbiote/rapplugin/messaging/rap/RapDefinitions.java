/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.rapplugin.messaging.rap;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class RapDefinitions {
    public static final String      PLUGIN_REGISTRATION_EXCHANGE_OUT = "symbIoTe.rapPluginExchange";
    public static final String      PLUGIN_REGISTRATION_KEY = "symbIoTe.rapPluginExchange.add-plugin"; // TODO This key is used for registration and unregistration. Its value should be changed to reflect that.

    public static final String      PLUGIN_EXCHANGE_IN = "plugin-exchange";
    
    public static final String      PLUGIN_NOTIFICATION_EXCHANGE_OUT = "symbIoTe.rapPluginExchange-notification";
    public static final String      PLUGIN_NOTIFICATION_KEY = "symbIoTe.rapPluginExchange.plugin-notification";
}
