package mecklon.scribbleIoClone.websocket;



import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mecklon.scribbleIoClone.dto.GameEventDTO;
import mecklon.scribbleIoClone.dto.GamePlayerDTO;
import mecklon.scribbleIoClone.dto.PlayerDTO;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import mecklon.scribbleIoClone.service.CustomUserDetails;
import mecklon.scribbleIoClone.service.GameRoomStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.config.annotation.web.WebAuthnDsl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {


    private final SimpMessagingTemplate messagingTemplate;
    private final  RedisTemplate<String,String> redisTemplate;


    @EventListener
    public void handleWebSocketConnectedListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (event.getUser() == null) return;
        Authentication auth = (Authentication)event.getUser();
        CustomUserDetails userDetails =(CustomUserDetails) auth.getPrincipal();
        redisTemplate.opsForHash().put(userDetails.getId()+":info", "sessionId", sessionId);
        String user = event.getUser() != null ? event.getUser().getName() : "Unknown";
        log.info("🔵 Connected: {}", user);
    }

    @EventListener
    public void handleWebSocketDisconnectedListener(SessionDisconnectEvent event) {
        String user = event.getUser() != null ? event.getUser().getName() : "Unknown";

        if(event.getUser()==null)return;
        Authentication auth = (Authentication)event.getUser();
        CustomUserDetails userDetails =(CustomUserDetails) auth.getPrincipal();

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String currentSessionId = accessor.getSessionId();

        String storedSessionId = (String)redisTemplate.opsForHash().get(userDetails.getId()+":info", "sessionId");

        //check if this websocket request is not stale
        if(!currentSessionId.equals(storedSessionId))return;

        //check if this user is currently in a room
        String roomId = (String)redisTemplate.opsForValue().get(userDetails.getId()+":room");
        if(roomId == null)return;

        String statusString = (String)redisTemplate.opsForHash().get(roomId+":info","status");
        if(statusString==null) return;
        GameRoomStatus status = GameRoomStatus.valueOf(statusString);

        // check if the game is in the correct state
        if(status == GameRoomStatus.DRAWING || status == GameRoomStatus.DRAWER_SELECTING_WORD){

            System.out.println("game status: "+status.toString());
            //check for duplicate request
            Long removed = redisTemplate.opsForSet().remove(roomId+":playersInGame",userDetails.getId());
            if(Objects.equals(removed, 1L)){
                redisTemplate.opsForSet().add("offlinePlayers",(String)userDetails.getId());
                redisTemplate.opsForHash().put(userDetails.getId()+":info", "offlineSince", ""+System.currentTimeMillis());
                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        GameEventDTO.builder()
                                .initiator(PlayerDTO.builder().id(userDetails.getId()).build())
                                .type(GameEventType.PLAYER_DISCONNECTED)
                                .build()
                );
            }
        }else if(status == GameRoomStatus.LOBBY){
            redisTemplate.opsForSet().remove(roomId+":members",userDetails.getId());
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    GameEventDTO.builder()
                            .initiator(new PlayerDTO(userDetails.getId(),userDetails.getDisplayUsername(), userDetails.getUsername(),null))
                            .type(GameEventType.PLAYER_EXIT)
                            .build()
            );
        }
        log.info("🔴 Disconnected: {}", user);
    }
}
