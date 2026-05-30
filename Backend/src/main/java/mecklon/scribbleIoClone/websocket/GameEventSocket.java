package mecklon.scribbleIoClone.websocket;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mecklon.scribbleIoClone.dto.*;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import mecklon.scribbleIoClone.service.CustomUserDetails;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import tools.jackson.databind.ObjectMapper;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameEventSocket {

    String[] words = {
            "apple",
            "bicycle",
            "volcano",
            "guitar",
            "elephant",
            "castle",
            "umbrella",
            "balloon",
            "mountain",
            "river",
            "diamond",
            "tornado",
            "compass",
            "pyramid",
            "cactus",
            "backpack",
            "skyscraper",
            "kangaroo",
            "submarine",
            "treasure",
            "ice cream",
            "traffic light",
            "birthday cake",
            "roller coaster",
            "fire extinguisher",
            "solar system",
            "washing machine",
            "coffee mug",
            "shopping cart",
            "tooth brush",
            "swimming pool",
            "movie theater",
            "parking lot",
            "video game",
            "snowman",
            "rainbow",
            "camp fire",
            "basket ball",
            "baseball bat",
            "tennis racket",
            "school bus",
            "police car",
            "air plane",
            "hot air balloon",
            "frying pan",
            "alarm clock",
            "water bottle",
            "sun glasses",
            "book shelf",
            "computer mouse"
    };

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

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
