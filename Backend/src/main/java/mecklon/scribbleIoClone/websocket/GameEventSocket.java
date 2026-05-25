package mecklon.scribbleIoClone.websocket;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mecklon.scribbleIoClone.dto.GameEventDTO;
import mecklon.scribbleIoClone.dto.SettingsChangeDTO;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameEventSocket {

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    @MessageMapping("/settingsChange")
    public void propogateRoomSettingsUpdate(@Payload SettingsChangeDTO settings, Principal principal){
        redisTemplate.opsForHash().put(settings.getRoomId() + ":info", "rounds", settings.getRounds()+"");
        redisTemplate.opsForHash().put(settings.getRoomId() + ":info", "timePerRound", settings.getTimePerRound()+"");
        messagingTemplate.convertAndSend(
                "/topic/room/" + settings.getRoomId(),
                GameEventDTO.builder()
                        .initiator(null)
                        .type(GameEventType.SETTINGS_CHANGED)
                        .data(Map.of(
                                "rounds",settings.getRounds(),
                                "timePerRound", settings.getTimePerRound()))
                        .build()
        );
        System.out.println(settings.getRounds());
    }
}
