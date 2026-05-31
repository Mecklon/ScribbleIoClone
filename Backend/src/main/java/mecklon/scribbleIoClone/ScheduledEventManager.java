package mecklon.scribbleIoClone;


import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.dto.GameEventDTO;
import mecklon.scribbleIoClone.dto.MessageSubmitResponse;
import mecklon.scribbleIoClone.dto.PlayerDTO;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import mecklon.scribbleIoClone.service.GameRoomStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ScheduledEventManager {
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String,String> redisTemplate;
    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper;

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
            }if(status == GameRoomStatus.DRAWING){
                String drawerId = (String)redisTemplate.opsForHash().get(roomId+":info","drawerId");
                redisTemplate.delete(roomId+":currentWords");
                redisTemplate.opsForHash().put(roomId+":info", "currentWord", null);
                Set<String> membersSet = redisTemplate.opsForSet().members(roomId+":members");
                Set<String> exitedMembersSet = redisTemplate.opsForSet().members(roomId+":exitedMembers");
                Set<ZSetOperations.TypedTuple<String>> roundPoints = redisTemplate.opsForZSet().rangeWithScores(roomId + ":roundPoints", 0, -1);
                redisTemplate.delete(roomId + ":roundPoints");
                Map<String,Integer> roundPointsMap = new HashMap<>();
                for(ZSetOperations.TypedTuple<String> entry : roundPoints){
                    roundPointsMap.put(
                            entry.getValue(),
                            entry.getScore().intValue()
                    );
                }
                List<Map<String,Object>> pointsOutput = new ArrayList<>();
                Integer totalRoundPoints = 0;
                for(String playerId: membersSet){
                    if(exitedMembersSet.contains(playerId) || Objects.equals(playerId, drawerId))continue;
                    String json = (String) redisTemplate.opsForHash().get(roomId + ":playerMeta", playerId);
                    PlayerDTO dto = objectMapper.readValue(
                            json,
                            PlayerDTO.class
                    );
                    Map<String,Object> playerAndPoints = new HashMap<>();
                    Integer playerPoints = roundPointsMap.getOrDefault(playerId,0);
                    playerAndPoints.put("points", playerPoints);
                    totalRoundPoints+=playerPoints;
                    playerAndPoints.put("username",dto.getUsername());
                    playerAndPoints.put("id",dto.getId());
                    pointsOutput.add(playerAndPoints);
                    if(playerPoints==0)continue;
                    redisTemplate.opsForZSet().incrementScore(roomId+":leaderboard", playerId,playerPoints);
                }
                String drawer = (String)redisTemplate.opsForHash().get(roomId+":info","drawer");
                pointsOutput.add(Map.of("points", totalRoundPoints/2, "username", drawer, "id",drawerId));
                redisTemplate.opsForZSet().incrementScore(roomId+":leaderboard", drawerId, (Integer)(totalRoundPoints/2));

                Integer drawerIndex = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "drawerIndex"));
                drawerIndex++;
                Set<String> newDrawerIdSet = redisTemplate.opsForZSet().range(roomId+":turnOrder",drawerIndex, drawerIndex);
                boolean reachedEnd = false;
                if(newDrawerIdSet.size()==0){
                    reachedEnd = true;
                }
                String newDrawerId = null;
                for(String newId: newDrawerIdSet) newDrawerId = newId;
                if(!reachedEnd){
                    while(exitedMembersSet.contains(newDrawerId)){
                        drawerIndex++;
                        newDrawerIdSet = redisTemplate.opsForZSet().range(roomId+":turnOrder",drawerIndex, drawerIndex);
                        if(newDrawerIdSet==null || newDrawerIdSet.size()==0){
                            reachedEnd = true;
                            break;
                        }
                        for(String newId: newDrawerIdSet) newDrawerId = newId;
                    }
                }
                Long phaseDeadLine = Long.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","phaseDeadLine"));

                if(reachedEnd){
                    Integer currentRound = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","currentRound"));
                    Integer totalRounds = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","rounds"));
                    if(currentRound == totalRounds){
                        phaseDeadLine = (Long)(System.currentTimeMillis()+60_000);
                        redisTemplate.opsForHash().putAll(roomId+":info",Map.of("phaseDeadLine", phaseDeadLine+"", "status", GameRoomStatus.ENDED.name()));
                        messagingTemplate.convertAndSend(
                                "/topic/room/" + roomId,
                                GameEventDTO.builder()
                                        .initiator(null)
                                        .type(GameEventType.GAME_END)
                                        .build()
                        );
                    }else{
                        currentRound++;
                        redisTemplate.opsForHash().put(roomId+":info","currentRound",currentRound+"");
                        drawerIndex=0;
                        newDrawerIdSet = redisTemplate.opsForZSet().range(roomId+":turnOrder",drawerIndex, drawerIndex);
                        for(String newId: newDrawerIdSet) newDrawerId = newId;

                        while(exitedMembersSet.contains(newDrawerId)){
                            newDrawerIdSet = redisTemplate.opsForZSet().range(roomId+":turnOrder",drawerIndex, drawerIndex);
                            drawerIndex++;
                            System.out.println("new round: "+newDrawerIdSet);
                            if(newDrawerIdSet == null || newDrawerIdSet.size()==0){
                                continue;
                            }
                            for(String newId: newDrawerIdSet) newDrawerId = newId;
                        }
                        String newDrawerJson = (String)redisTemplate.opsForHash().get(roomId+":playerMeta", newDrawerId);
                        PlayerDTO newDrawerDTO = objectMapper.readValue(newDrawerJson, PlayerDTO.class);
                        redisTemplate.opsForHash().put(roomId+":info","drawerId", newDrawerId);
                        redisTemplate.opsForHash().put(roomId+":info","drawer", newDrawerDTO.getUsername());
                        redisTemplate.opsForHash().put(roomId+":info","drawerIndex", drawerIndex+"");
                        redisTemplate.opsForHash().put(roomId+":info","status", GameRoomStatus.DRAWER_SELECTING_WORD.name());
                        Long newPhaseDeadLine = 25_000+System.currentTimeMillis();
                        redisTemplate.opsForHash().put(roomId+":info","phaseDeadLine", newPhaseDeadLine+"");
                        messagingTemplate.convertAndSend(
                                "/topic/room/" + roomId,
                                GameEventDTO.builder()
                                        .initiator(null)
                                        .type(GameEventType.ROUND_END)
                                        .data(
                                                Map.of("phaseDeadLine",newPhaseDeadLine,
                                                        "drawerId", newDrawerId,
                                                        "drawer",newDrawerDTO.getUsername(),
                                                        "points",pointsOutput,
                                                        "newRoundIndex",currentRound
                                                )
                                        )
                                        .build()
                        );
                    }
                }else{
                    String newDrawerJson = (String)redisTemplate.opsForHash().get(roomId+":playerMeta", newDrawerId);
                    PlayerDTO newDrawerDTO = objectMapper.readValue(newDrawerJson, PlayerDTO.class);
                    redisTemplate.opsForHash().put(roomId+":info","drawerId", newDrawerId);
                    redisTemplate.opsForHash().put(roomId+":info","drawer", newDrawerDTO.getUsername());
                    redisTemplate.opsForHash().put(roomId+":info","drawerIndex", drawerIndex+"");
                    redisTemplate.opsForHash().put(roomId+":info","status", GameRoomStatus.DRAWER_SELECTING_WORD.name());
                    Long newPhaseDeadLine = 20_000+System.currentTimeMillis();
                    redisTemplate.opsForHash().put(roomId+":info","phaseDeadLine", phaseDeadLine+"");

                    messagingTemplate.convertAndSend(
                            "/topic/room/" + roomId,
                            GameEventDTO.builder()
                                    .initiator(null)
                                    .type(GameEventType.DRAWER_SELECTING_WORD)
                                    .data(
                                            Map.of("phaseDeadLine",newPhaseDeadLine,
                                                    "drawerId", newDrawerId,
                                                    "drawer",newDrawerDTO.getUsername(),
                                                    "points",pointsOutput
                                            )
                                    )
                                    .build()
                    );

                }

            }else if(status == GameRoomStatus.ENDED){
                // nuke everything in redis with the prefix {roomid}
            }
        }
    }
}
