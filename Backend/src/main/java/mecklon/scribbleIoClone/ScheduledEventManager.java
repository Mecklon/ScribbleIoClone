package mecklon.scribbleIoClone;


import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.dto.GameEventDTO;
import mecklon.scribbleIoClone.dto.PlayerDTO;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import mecklon.scribbleIoClone.service.GameRoomStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class ScheduledEventManager {
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String,String> redisTemplate;
    private final SecureRandom random = new SecureRandom();

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

    @Scheduled(fixedDelay = 5000)
    public void propogateNextPhase(){
        Set<String> roomIds = redisTemplate.opsForSet().members("rooms");
        for(String roomId: roomIds){
            String deadlineString = (String)redisTemplate.opsForHash().get(roomId+":info","phaseDeadLine");
            if(deadlineString==null)continue;
            Long deadline = Long.parseLong(deadlineString);
            if(deadline>System.currentTimeMillis()) continue;
            GameRoomStatus status = GameRoomStatus.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "status"));
            if(status == GameRoomStatus.PLAYERS_SWITCHING_TO_GAME){
                redisTemplate.opsForHash().put(roomId+":info","status",GameRoomStatus.DRAWER_SELECTING_WORD.name());
                Long newDeadLine = System.currentTimeMillis()+20_000;
                redisTemplate.opsForHash().put(roomId+":info","phaseDeadLine", String.valueOf(newDeadLine));
                String drawerId = (String)redisTemplate.opsForHash().get(roomId+":info","drawerId");
                String drawer = (String)redisTemplate.opsForHash().get(roomId+":info","drawer");

                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        GameEventDTO.builder()
                                .initiator(null)
                                .type(GameEventType.DRAWER_SELECTING_WORD)
                                .data(
                                        Map.of("phaseDeadLine",newDeadLine,
                                                "drawerId", drawerId,
                                                "drawer",drawer
                                        )
                                )
                                .build()
                );
            }if(status == GameRoomStatus.DRAWER_SELECTING_WORD){
                redisTemplate.opsForHash().put(roomId+":info", "status", GameRoomStatus.DRAWING.name());
                Long timePerRound = Long.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "timePerRound" ));
                Long newDeadLine = System.currentTimeMillis() + timePerRound * 1000;

                Set<String> wordsFromRedis = redisTemplate.opsForSet().members(roomId+":currentWords");
                ArrayList<String> output = new ArrayList<>();

                if(!wordsFromRedis.isEmpty()){
                    int count = 0;
                    output.addAll(wordsFromRedis);
                }
                for(int i =0;i< 3;i++){
                    Boolean customWordsOnly = Boolean.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "customWordsOnly"));
                    Set<String> customWords = redisTemplate.opsForSet().members(roomId+":customWords");
                    ArrayList<String> listOfCustomWords = new ArrayList<>(customWords);
                    if(!customWordsOnly){
                        output.add(words[random.nextInt(words.length)]);
                    }else{
                        if(random.nextInt(10)>5){
                            output.add(words[random.nextInt(words.length)]);
                        }else{
                            output.add(listOfCustomWords.get(random.nextInt(listOfCustomWords.size())));
                        }
                    }
                    redisTemplate.opsForSet().add(roomId+":currentWords", output.getLast());
                }

                String currentWord = output.get(random.nextInt(output.size()));
                redisTemplate.opsForHash().put(roomId+":info","currentWord", currentWord);
                String drawer = (String)redisTemplate.opsForHash().get(roomId+":info","drawer");
                String drawerId = (String)redisTemplate.opsForHash().get(roomId+":info","drawerId");
                String hiddenWord = currentWord.replaceAll("[^\\s]", "_");

                redisTemplate.opsForHash().put(roomId + ":info", "phaseDeadLine", String.valueOf(newDeadLine));

                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        GameEventDTO.builder()
                                .initiator(
                                        new PlayerDTO(
                                                drawerId,
                                                drawer,
                                                null,
                                                null
                                        )
                                )
                                .type(GameEventType.PLAYER_DRAWING)
                                .data(Map.of(
                                        "word",hiddenWord,
                                        "phaseDeadLine",newDeadLine
                                ))
                                .build()
                );
            }
        }
    }
}
