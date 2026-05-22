package mecklon.scribbleIoClone.service;

import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.dto.GameEventDTO;
import mecklon.scribbleIoClone.dto.PlayerDTO;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import mecklon.scribbleIoClone.model.User;
import mecklon.scribbleIoClone.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    String codeReference = "abcdefghijklmnopqrstuvwzyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";

    public String createRoom(Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        StringBuilder idSb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            idSb.append(codeReference.charAt(random.nextInt(codeReference.length())));
        }
        String id = idSb.toString();

        User user = userRepository.findByEmail(userDetails.getUsername());
        PlayerDTO newPlayer = new PlayerDTO(
                user.getUsername(),
                userDetails.getUsername(),
                user.getFileName()
        );

        redisTemplate.opsForHash().put(
                id + ":playerMeta",
                userDetails.getUsername(),
                objectMapper.writeValueAsString(newPlayer)
        );

        redisTemplate.opsForHash().put(id + ":info", "host", userDetails.getUsername());
        redisTemplate.opsForHash().put(id + ":info", "memberCount", 1+"");
        redisTemplate.opsForHash().put(id + ":info", "status", "LOBBY");
        redisTemplate.opsForHash().put(id + ":info", "createdAt", LocalDateTime.now().toString());

        redisTemplate.opsForZSet().add(id + ":leaderboard", userDetails.getUsername(), 0);

        redisTemplate.opsForSet().add(id + ":members", userDetails.getUsername());

        redisTemplate.opsForZSet().add(id + ":turnOrder", userDetails.getUsername(), 1);

        return id;
    }

    public List<PlayerDTO> joinRoom(Authentication auth, String roomId){
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        Long added = redisTemplate.opsForSet().add(roomId + ":members", userDetails.getUsername());

        if(added==1){
            User user = userRepository.findByEmail(userDetails.getUsername());

            PlayerDTO newPlayer = new PlayerDTO(
                    user.getUsername(),
                    userDetails.getUsername(),
                    user.getFileName()
            );

            redisTemplate.opsForHash().put(
                    roomId + ":playerMeta",
                    userDetails.getUsername(),
                    objectMapper.writeValueAsString(newPlayer)
            );

            Long members = redisTemplate.opsForHash().increment(
                    roomId + ":info",
                    "memberCount",
                    1
            );

            redisTemplate.opsForZSet().add(
                    roomId + ":leaderboard",
                    userDetails.getUsername(),
                    0
            );

            redisTemplate.opsForZSet().add(
                    roomId + ":turnOrder",
                    userDetails.getUsername(),
                    members
            );

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    GameEventDTO.builder()
                            .initiator(newPlayer)
                            .type(GameEventType.NEW_MEMBER_JOINED)
                            .build()
            );
        }

        Set<String> playerEmails = redisTemplate.opsForSet()
                .members(roomId + ":members");

        List<PlayerDTO> res = new ArrayList<>();

        for(String playerEmail: playerEmails){
            String json = (String)
                    redisTemplate.opsForHash()
                            .get(roomId + ":playerMeta", playerEmail);

            PlayerDTO dto = objectMapper.readValue(
                    json,
                    PlayerDTO.class
            );

            res.add(dto);
        }

        return res;
    }
}
