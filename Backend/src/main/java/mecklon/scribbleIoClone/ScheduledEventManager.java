package mecklon.scribbleIoClone;


import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.configuration.GamePropertiesService;
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
    private final GamePropertiesService gamePropertiesService;

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
        currentRoomProcess: for(String roomId: roomIds){
            String deadlineString = (String)redisTemplate.opsForHash().get(roomId+":info","phaseDeadLine");
            if(deadlineString==null)continue;
            Long deadline = Long.parseLong(deadlineString);
            if(deadline>System.currentTimeMillis()) continue;
            GameRoomStatus status = GameRoomStatus.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "status"));
            if(status == GameRoomStatus.PLAYERS_SWITCHING_TO_GAME){
                System.out.println("PLAYERS_SWITCHING_TO_GAME read: "+deadline);
                redisTemplate.opsForHash().put(roomId+":info","status",GameRoomStatus.DRAWER_SELECTING_WORD.name());
                Long newDeadLine = gamePropertiesService.getLobbySwitchDeadline();
                System.out.println("PLAYERS_SWITCHING_TO_GAME sets: "+newDeadLine);
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
                System.out.println("DRAWER_SELECTING_WORD read: "+deadline);
                redisTemplate.opsForHash().put(roomId+":info", "status", GameRoomStatus.DRAWING.name());
                Long timePerRound = Long.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "timePerRound" ));
                Long newDeadLine = System.currentTimeMillis() + timePerRound * 1000;

                Set<String> wordsFromRedis = redisTemplate.opsForSet().members(roomId+":currentWords");
                ArrayList<String> output = new ArrayList<>();
                System.out.println("reading room = " + roomId);
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
                System.out.println("DRAWER_SELECTING_WORD sets: "+newDeadLine);
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
                System.out.println("DRAWING read: "+deadline);

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
                Long phaseDeadLine;

                if(reachedEnd){
                    Integer currentRound = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","currentRound"));
                    Integer totalRounds = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","rounds"));
                    if(currentRound == totalRounds){
                        phaseDeadLine = gamePropertiesService.GameDropDeadline();
                        System.out.println("DRAWING ends game sets: "+phaseDeadLine);
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
                            System.out.println("new round: "+newDrawerIdSet+" if size 0 then game ends");
                            if(newDrawerIdSet == null || newDrawerIdSet.size()==0){
                                // game ends because every one leaves
                                System.out.println("game ends");
                                phaseDeadLine = gamePropertiesService.GameDropDeadline();
                                redisTemplate.opsForHash().putAll(roomId+":info",Map.of("phaseDeadLine", phaseDeadLine+"", "status", GameRoomStatus.ENDED.name()));
                                continue currentRoomProcess;
                            }
                            for(String newId: newDrawerIdSet) newDrawerId = newId;
                        }
                        String newDrawerJson = (String)redisTemplate.opsForHash().get(roomId+":playerMeta", newDrawerId);
                        PlayerDTO newDrawerDTO = objectMapper.readValue(newDrawerJson, PlayerDTO.class);
                        redisTemplate.opsForHash().put(roomId+":info","drawerId", newDrawerId);
                        redisTemplate.opsForHash().put(roomId+":info","drawer", newDrawerDTO.getUsername());
                        redisTemplate.opsForHash().put(roomId+":info","drawerIndex", drawerIndex+"");
                        redisTemplate.opsForHash().put(roomId+":info","status", GameRoomStatus.DRAWER_SELECTING_WORD.name());
                        redisTemplate.delete(roomId+":canvasEvents");
                        Long newPhaseDeadLine = gamePropertiesService.getRoundEndDeadline();
                        System.out.println("DRAWING ends round sets: "+newPhaseDeadLine);
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
                    redisTemplate.delete(roomId+":canvasEvents");
                    System.out.println("writing room = " + roomId);
                    Long newPhaseDeadLine = gamePropertiesService.getDrawerSelectDeadline();
                    System.out.println("DRAWING next player sets: "+newPhaseDeadLine);
                    redisTemplate.opsForHash().put(roomId+":info","phaseDeadLine", newPhaseDeadLine+"");

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

                List<String> keys = List.of(
                        roomId+":leaderboard",
                        roomId+":members",
                        roomId+":info",
                        roomId+":playerMeta",
                        roomId+":playersInGame",
                        roomId+":playerPoints",
                        roomId+":offlineSince",
                        roomId+":currentWords",
                        roomId+":chat",
                        roomId+":turnOrder",
                        roomId+":customWords",
                        roomId+":canvasEvents",
                        roomId+":exitedMembers",
                        roomId+":roundPoints"
                );
                if(keys != null && !keys.isEmpty()){
                    redisTemplate.delete(keys);
                }
            }
        }
    }
    @Scheduled(fixedDelay = 5000)
    public void cleanUpOfflinePlayers(){
        Set<String> players = redisTemplate.opsForSet().members("offlinePlayers");

        if(players == null || players.isEmpty()) return;
        for(String playerId : players){

            // check per character keep the window for stale data small as possible, stale value is read which happends when use reconnects but now the blow code still disconnects them meaning they are now kicked out
            String deadlineString = (String)redisTemplate.opsForHash().get(playerId+":info", "offlineSince");
            if(deadlineString == null)continue;

            Long deadline = Long.valueOf(deadlineString)+60_000;
            if(System.currentTimeMillis() < deadline) continue;


            String roomId = redisTemplate.opsForValue().get(playerId+":room");
            if(roomId==null)continue;

            GameRoomStatus status = GameRoomStatus.valueOf((String) redisTemplate.opsForHash().get(roomId+":info","status"));
            if(status != GameRoomStatus.DRAWING && status!=GameRoomStatus.DRAWER_SELECTING_WORD ) continue;

            redisTemplate.delete(playerId+":room");
            redisTemplate.opsForSet().add(roomId+":exitedMembers", playerId);
            redisTemplate.opsForSet().remove("offlinePlayers",playerId);
            redisTemplate.opsForSet().remove(roomId+":playersInGame",playerId);
            redisTemplate.opsForHash().delete(playerId + ":info", "offlineSince");

            String json = (String)redisTemplate.opsForHash().get(roomId+":playerMeta", playerId);
            if(json == null) continue;
            PlayerDTO player = objectMapper.readValue(json, PlayerDTO.class);


            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    GameEventDTO.builder()
                            .initiator(new PlayerDTO(player.getId(),player.getUsername(), player.getEmail(),null))
                            .type(GameEventType.PLAYER_EXIT)
                            .data(Map.of("message",player.getUsername() + "was kicked out due to disconnect timeout"))
                            .build()
            );
        }
    }
}
