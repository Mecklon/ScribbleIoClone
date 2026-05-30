package mecklon.scribbleIoClone.websocket;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mecklon.scribbleIoClone.dto.GameEventDTO;
import mecklon.scribbleIoClone.dto.PlayerDTO;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import mecklon.scribbleIoClone.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.config.annotation.web.WebAuthnDsl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {


    private final SimpMessagingTemplate messagingTemplate;
    private final  RedisTemplate<String,String> redisTemplate;


    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectEvent event) {
        String user = event.getUser() != null ? event.getUser().getName() : "Unknown";
        log.info("🔵 Connected: {}", user);
    }

    @EventListener
    public void handleWebSocketDisconnectedListener(SessionDisconnectEvent event) {
        String user = event.getUser() != null ? event.getUser().getName() : "Unknown";
        if(event.getUser()==null)return;
        Authentication auth = (Authentication)event.getUser();
        CustomUserDetails userDetails =(CustomUserDetails) auth.getPrincipal();
        String roomId = (String)redisTemplate.opsForValue().get(userDetails.getId()+":room");
        redisTemplate.opsForHash().put(roomId + ":offlineSince", userDetails.getId(), String.valueOf(System.currentTimeMillis()));
        if(roomId!=null){
            Long removed = redisTemplate.opsForSet().remove(roomId+":playersInGame",userDetails.getId());
            if(Objects.equals(removed, 1L)){
                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        GameEventDTO.builder()
                                .initiator(PlayerDTO.builder().id(userDetails.getId()).build())
                                .type(GameEventType.PLAYER_DISCONNECTED)
                                .build()
                );
            }
        }
        log.info("🔴 Disconnected: {}", user);
    }
}
