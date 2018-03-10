package eu.h2020.symbiote.rapplugin.rap.plugin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import eu.h2020.symbiote.cloud.model.data.Result;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.rapplugin.messaging.rap.NotificationResourceListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.RapPlugin;
import eu.h2020.symbiote.rapplugin.messaging.rap.ReadingResourceListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.ActuatingResourceListener;
import eu.h2020.symbiote.rapplugin.messaging.rap.InvokingServiceListener;

@RunWith(MockitoJUnitRunner.class)
public class RapPluginTest {

    @Mock
    private ReadingResourceListener readingListener;
    
    @Mock
    private ActuatingResourceListener actuatingListener;

    @Mock
    private InvokingServiceListener invokingServiceListener;

    @Mock
    private NotificationResourceListener notificationListener;

    @Test
    public void callingReadingResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doReadResource("resourceId");
        })
            //then
        // new RuntimeException("ReadingResourceListener not registered in RapPlugin");
            .isInstanceOf(RuntimeException.class)
            .hasMessage("ReadingResourceListener not registered in RapPlugin")
            .hasNoCause();
    }

    @Test
    public void callingReadingResourceWhenUnregisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        plugin.registerReadingResourceListener(readingListener);
        plugin.unregisterReadingResourceListener(readingListener);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doReadResource("resourceId");
        })
        //then
        // new RuntimeException("ReadingResourceListener not registered in RapPlugin");
        .isInstanceOf(RuntimeException.class)
        .hasMessage("ReadingResourceListener not registered in RapPlugin")
        .hasNoCause();
    }
    
    @Test
    public void registeringAndCallingReadingResource_shouldCallListener() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        Observation expectedObservation = Mockito.mock(Observation.class);
        when(readingListener.readResource("resourceId")).thenReturn(expectedObservation);
        plugin.registerReadingResourceListener(readingListener);
        
        // when
        Observation result = plugin.doReadResource("resourceId");
        
        //then
        assertThat(result).isSameAs(expectedObservation);
    }
    
    @Test
    public void callingReadingResourceHistoryWhenNotRegisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doReadResourceHistory("resourceId");
        })
            //then
        // new RuntimeException("ReadingResourceListener not registered in RapPlugin");
            .isInstanceOf(RuntimeException.class)
            .hasMessage("ReadingResourceListener not registered in RapPlugin")
            .hasNoCause();
    }
    
    @Test
    public void registeringAndCallingReadingResourceHistory_shouldCallListener() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        LinkedList<Observation> expectedResult = new LinkedList<>();
        when(readingListener.readResourceHistory("resourceId")).thenReturn(expectedResult);
        plugin.registerReadingResourceListener(readingListener);
        
        // when
        List<Observation> result = plugin.doReadResourceHistory("resourceId");
        
        //then
        assertThat(result).isSameAs(expectedResult);
    }
    
    @Test
    public void callingWritingResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doActuateResource("resourceId", null);
        })
            //then
            .isInstanceOf(RuntimeException.class)
            .hasMessage("ActuatingResourceListener not registered in RapPlugin")
            .hasNoCause();
    }

    @Test
    public void callingWritingResourceWhenUnregisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        plugin.registerActuatingResourceListener(actuatingListener);
        plugin.unregisterActuatingResourceListener(actuatingListener);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doActuateResource("resourceId", null);
        })
        //then
        .isInstanceOf(RuntimeException.class)
        .hasMessage("ActuatingResourceListener not registered in RapPlugin")
        .hasNoCause();
    }
    
    @Test
    public void registeringAndCallingActuatingResource_shouldCallListener() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        plugin.registerActuatingResourceListener(actuatingListener);
        
        // when
        plugin.doActuateResource("resourceId", null);
        
        //then
    }

    @Test
    public void callingInvokingServiceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doInvokeService("resourceId", null);
        })
            //then
            .isInstanceOf(RuntimeException.class)
            .hasMessage("InvokingServiceListener not registered in RapPlugin")
            .hasNoCause();
    }

    @Test
    public void callingInvokingServiceWhenUnregisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        plugin.registerInvokingServiceListener(invokingServiceListener);
        plugin.unregisterInvokingServiceListener(invokingServiceListener);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doInvokeService("resourceId", null);
        })
        //then
        .isInstanceOf(RuntimeException.class)
        .hasMessage("InvokingServiceListener not registered in RapPlugin")
        .hasNoCause();
    }

    @Test
    public void registeringAndCallingInvokingService_shouldCallListener() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        Object expectedResult = new Object();
        plugin.registerInvokingServiceListener(invokingServiceListener);
        when(invokingServiceListener.invokeService("resourceId", null)).thenReturn(expectedResult);
        
        // when
        Object result = plugin.doInvokeService("resourceId", null);
        
        //then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void callingSubcribeResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doSubscribeResource("resourceId");
        })
            //then
            .isInstanceOf(RuntimeException.class)
            .hasMessage("NotificationResourceListener not registered in RapPlugin")
            .hasNoCause();
    }

    @Test
    public void callingSubscribeResourceWhenUnregisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        plugin.registerNotificationResourceListener(notificationListener);
        plugin.unregisterNotificationResourceListener(notificationListener);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doSubscribeResource("resourceId");
        })
        //then
        .isInstanceOf(RuntimeException.class)
        .hasMessage("NotificationResourceListener not registered in RapPlugin")
        .hasNoCause();
    }
    
    @Test
    public void registeringAndCallingSubscribeResource_shouldCallListener() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        plugin.registerNotificationResourceListener(notificationListener);
        
        // when
        plugin.doSubscribeResource("resourceId");
        
        //then
        verify(notificationListener).subscribeResource("resourceId");
    }

    @Test
    public void callingUnsubcribeResourceWhenNotRegisteredListener_shouldThrowException() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        
        assertThatThrownBy(() -> {
            // when
            plugin.doUnsubscribeResource("resourceId");
        })
            //then
            .isInstanceOf(RuntimeException.class)
            .hasMessage("NotificationResourceListener not registered in RapPlugin")
            .hasNoCause();
    }

    @Test
    public void registeringAndCallingUnsubscribeResource_shouldCallListener() throws Exception {
        //given
        RapPlugin plugin = new RapPlugin(null, "enablerName", false, false);
        plugin.registerNotificationResourceListener(notificationListener);
        
        // when
        plugin.doUnsubscribeResource("resourceId");
        
        //then
        verify(notificationListener).unsubscribeResource("resourceId");
    }
}
