package mecklon.scribbleIoClone.websocket;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@Slf4j
public class WebSocketEventListener {


    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectEvent event) {
        String user = event.getUser() != null ? event.getUser().getName() : "Unknown";
        log.info("🔵 Connected: {}", user);
    }

    @EventListener
    public void handleWebSocketDisconnectedListener(SessionDisconnectEvent event) {
        String user = event.getUser() != null ? event.getUser().getName() : "Unknown";
        log.info("🔴 Disconnected: {}", user);
    }
}
