package mecklon.scribbleIoClone.service;

import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.Exceptions.GameException;
import mecklon.scribbleIoClone.Exceptions.GameExceptionType;
import mecklon.scribbleIoClone.dto.*;
import mecklon.scribbleIoClone.dto.types.GameEventType;
import mecklon.scribbleIoClone.model.User;
import mecklon.scribbleIoClone.repository.UserRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GameService {
    private final RedisTemplate<String, String> redisTemplate;
    private final SecureRandom random = new SecureRandom();
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

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
                userDetails.getId(),
                user.getUsername(),
                userDetails.getUsername(),
                user.getFileName()
        );


        id = userDetails.getDisplayUsername()+":"+id;
        redisTemplate.opsForSet().add("rooms", id);
        redisTemplate.opsForHash().put(
                id + ":playerMeta",
                userDetails.getId(),
                objectMapper.writeValueAsString(newPlayer)
        );

        redisTemplate.opsForHash().put(id + ":info", "host", user.getId());
        redisTemplate.opsForHash().put(id + ":info", "memberCount", 1+"");
        redisTemplate.opsForHash().put(id + ":info", "status", GameRoomStatus.LOBBY.name());
        redisTemplate.opsForHash().put(id + ":info", "createdAt", LocalDateTime.now().toString());
        redisTemplate.opsForHash().put(id + ":info", "rounds", 3+"");
        redisTemplate.opsForHash().put(id + ":info", "timePerRound", 40+"");
        redisTemplate.opsForHash().put(id + ":info", "hostUsername", user.getUsername());
        redisTemplate.opsForHash().put(id + ":info", "currentRound", String.valueOf(1));
        redisTemplate.opsForHash().put(id + ":info", "drawerIndex", String.valueOf(0));


        redisTemplate.opsForHash().put(id+":playerPoints", user.getId(),0+"");

        redisTemplate.opsForZSet().add(id + ":leaderboard", userDetails.getUsername(), 0);

        redisTemplate.opsForSet().add(id + ":members", userDetails.getId());

        redisTemplate.opsForZSet().add(id + ":turnOrder", userDetails.getId(), 0);

        return id;
    }

    public RoomDetails joinRoom(Authentication auth, String roomId){
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        Long added = redisTemplate.opsForSet().add(roomId + ":members", userDetails.getId());
        Map<Object, Object> roomInfo = redisTemplate.opsForHash().entries(roomId+":info");
        String host = (String)roomInfo.get("host");
        int rounds = Integer.parseInt((String)roomInfo.get("rounds"));
        int timePerRound = Integer.parseInt((String)roomInfo.get("timePerRound"));
        String hostUsername = (String)roomInfo.get("hostUsername");

        if(added==1){
            User user = userRepository.findByEmail(userDetails.getUsername());

            PlayerDTO newPlayer = new PlayerDTO(
                    user.getId(),
                    user.getUsername(),
                    userDetails.getUsername(),
                    user.getFileName()
            );

            redisTemplate.opsForHash().put(
                    roomId + ":playerMeta",
                    userDetails.getId(),
                    objectMapper.writeValueAsString(newPlayer)
            );
            // could be replaced with getting the size of :member set, making member count obsolete
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
                    userDetails.getId(),
                    members
            );
            redisTemplate.opsForHash().put(
                    roomId+":playerPoints",
                    userDetails.getId(),
                    0+""
            );

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    GameEventDTO.builder()
                            .initiator(newPlayer)
                            .type(GameEventType.NEW_MEMBER_JOINED)
                            .build()
            );

        }

        redisTemplate.opsForValue().set(userDetails.getId()+":room",roomId);
        Set<String> playerIds = redisTemplate.opsForSet()
                .members(roomId + ":members");

        List<PlayerDTO> players = new ArrayList<>();

        for(String playerId : playerIds){
            String json = (String)
                    redisTemplate.opsForHash()
                            .get(roomId + ":playerMeta", playerId);
            PlayerDTO dto = objectMapper.readValue(
                    json,
                    PlayerDTO.class
            );

            players.add(dto);
        }
        return new RoomDetails(players, host, rounds, timePerRound, hostUsername);
    }

    public void startGame(GameStartRequest request, Authentication auth) {

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();

        String hostId = (String) redisTemplate.opsForHash().get(request.getRoomId() + ":info", "host");

        if (hostId == null) {
            throw new GameException(HttpStatus.BAD_REQUEST,GameExceptionType.ROOM_DOES_NOT_EXIST,"Room with id: " + request.getRoomId() + " does not exist");
        }

        if (!userDetails.getId().equals(hostId)) {
            throw new GameException(HttpStatus.FORBIDDEN,GameExceptionType.HOST_PRIVILEDGE_NOT_ALLOWED,"Only host can start the game");
        }

        String customWords = request.getCustomWords().toUpperCase();

        if (customWords != null && !customWords.isBlank()) {
            String[] words = customWords.trim().split(",");

            for (String word : words) {
                word = word.trim();

                if (word.length() < 3 || word.length() > 20) {
                    throw new GameException(
                            HttpStatus.BAD_REQUEST,
                            GameExceptionType.WORD_LENGTH_VIOLATION,
                            word + " violated the word length constraints"
                    );
                }

                if (!word.matches("^[A-Z ]+$")) {
                    throw new GameException(
                            HttpStatus.BAD_REQUEST,
                            GameExceptionType.INVALID_CUSTOM_WORD,
                            word + " contains invalid characters"
                    );
                }
            }

            for (String word : words) {
                redisTemplate.opsForSet()
                        .add(request.getRoomId() + ":customWords", word.trim().toLowerCase());
            }
        }


        redisTemplate.opsForHash().put(request.getRoomId() + ":info", "rounds", String.valueOf(request.getRounds()));
        redisTemplate.opsForHash().put(request.getRoomId() + ":info", "timePerRound", String.valueOf(request.getTimePerRound()));
        redisTemplate.opsForHash().put(request.getRoomId() + ":info", "status", GameRoomStatus.PLAYERS_SWITCHING_TO_GAME.name());
        redisTemplate.opsForHash().put(request.getRoomId() + ":info", "customWordsOnly", String.valueOf(request.getOnlyCustomWords()));
        redisTemplate.opsForSet().add(request.getRoomId() + ":playersInGame", userDetails.getId());

        Long phaseDeadLine = System.currentTimeMillis() + 25_000;
        redisTemplate.opsForHash().put(request.getRoomId() + ":info", "phaseDeadLine", String.valueOf(phaseDeadLine));

        messagingTemplate.convertAndSend(
                "/topic/room/" + request.getRoomId(),
                GameEventDTO.builder()
                        .initiator(
                                new PlayerDTO(
                                        userDetails.getId(),
                                        userDetails.getDisplayUsername(),
                                        null,
                                        null
                                )
                        )
                        .type(GameEventType.PLAYERS_SWITCHING_TO_GAME)
                        .build()
        );
    }


    public RoomSnapshot joinGame(String roomId, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails)auth.getPrincipal();
        Boolean isMember = redisTemplate.opsForSet().isMember(roomId+":members",userDetails.getId());
        if(!isMember){
            throw new GameException(HttpStatus.FORBIDDEN, GameExceptionType.USER_NOT_PART_OF_GROUP, "You are not a player of this group");
        }
        Long added = redisTemplate.opsForSet().add(roomId+":playersInGame",userDetails.getId());
        redisTemplate.opsForHash().delete(roomId + ":offlineSince", userDetails.getId());
        if(!Objects.equals(added, 0L)){
            Long totalSize = redisTemplate.opsForSet().size(roomId + ":members");
            Long currentSize = redisTemplate.opsForSet().size(roomId+":playersInGame");

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    GameEventDTO.builder()
                            .initiator(
                                    new PlayerDTO(
                                            userDetails.getId(),
                                            userDetails.getDisplayUsername(),
                                            null,
                                            null
                                    )
                            )
                            .type(GameEventType.MEMBER_JOINED_GAME)
                            .build()
            );
            GameRoomStatus gameStatus = GameRoomStatus.valueOf((String)redisTemplate.opsForHash().get(roomId + ":info", "status"));
            if(gameStatus == GameRoomStatus.PLAYERS_SWITCHING_TO_GAME && Objects.equals(totalSize, currentSize)){
                redisTemplate.opsForHash().put(roomId+":info","status", GameRoomStatus.DRAWER_SELECTING_WORD.name());
                Long newDeadLine = System.currentTimeMillis()+20_000;
                redisTemplate.opsForHash().put(roomId+":info","phaseDeadLine", String.valueOf(newDeadLine));
                String drawerId=null;
                Integer drawerIndex = null;
                Boolean isPresent = false;
                do{
                    drawerIndex = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","drawerIndex"));
                    Set<String> currentPlayerSet =redisTemplate.opsForZSet().rangeByScore(roomId + ":turnOrder",drawerIndex, drawerIndex);
                    drawerId = null;
                    for(String curr: currentPlayerSet)drawerId = curr;
                    isPresent = !redisTemplate.opsForSet().isMember(roomId+":exitedMembers",drawerId);

                }while(!isPresent);

                String drawerJson = (String)redisTemplate.opsForHash().get(roomId+":playerMeta", drawerId);
                PlayerDTO drawer = objectMapper.readValue(drawerJson, PlayerDTO.class);
                redisTemplate.opsForHash().put(roomId+":info","drawerIndex", drawerIndex+"");
                redisTemplate.opsForHash().put(roomId+":info","drawerId", drawerId);
                redisTemplate.opsForHash().put(roomId+":info","drawer", drawer.getUsername());

                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        GameEventDTO.builder()
                                .initiator(null)
                                .type(GameEventType.DRAWER_SELECTING_WORD)
                                .data(
                                        Map.of("phaseDeadLine",String.valueOf(newDeadLine),
                                        "drawerId", drawer.getId(),
                                        "drawer",drawer.getUsername()
                                        )
                                )
                                .build()
                );
            }
        }

        Set<String> playerIds = redisTemplate.opsForSet()
                .members(roomId + ":members");

        List<GamePlayerDTO> players = new ArrayList<>();

        Set<String> connectedPlayers = redisTemplate.opsForSet().members(roomId+":playersInGame");
        Set<ZSetOperations.TypedTuple<String>> leaderboard = redisTemplate.opsForZSet().reverseRangeWithScores(roomId + ":leaderboard", 0, -1);
        Map<String,Integer> pointsMap = new HashMap<>();

        for(ZSetOperations.TypedTuple<String> entry : leaderboard){
            pointsMap.put(
                    entry.getValue(),
                    entry.getScore().intValue()
            );
        }

        Map<Object,Object> playerInfo = redisTemplate.opsForHash().entries(roomId + ":playerMeta");

        for(String playerId : playerIds){
            String json = (String) playerInfo.get(playerId);
            PlayerDTO dto = objectMapper.readValue(
                    json,
                    PlayerDTO.class
            );
            Boolean isConnected = connectedPlayers.contains(playerId);
            Integer points = pointsMap.getOrDefault(playerId, 0);
            players.add(new GamePlayerDTO(dto, points, isConnected));
        }
        Map<Object, Object> roomInfo = redisTemplate.opsForHash().entries(roomId+":info");
        String host = (String)roomInfo.get("host");
        String hostUsername = (String)roomInfo.get("hostUsername");
        Integer rounds = Integer.parseInt((String)roomInfo.get("rounds"));
        Integer timePerRound = Integer.parseInt((String)roomInfo.get("timePerRound"));

        List<String> stringChats = redisTemplate.opsForList().range(roomId+":chat",0,-1);
        List<GameChatMessage> chats = new ArrayList<>();
        for(String stringChat: stringChats){
            chats.add(objectMapper.readValue(stringChat, GameChatMessage.class));
        }

        GameRoomStatus gameStatus = GameRoomStatus.valueOf((String)redisTemplate.opsForHash().get(roomId + ":info", "status"));

        String phaseDeadLineString = (String)redisTemplate.opsForHash().get(roomId+":info","phaseDeadLine");
        Long phaseDeadLine = null;
        if(phaseDeadLineString !=null){
            phaseDeadLine = Long.parseLong(phaseDeadLineString);
        }


        Integer currentRound = Integer.parseInt((String)redisTemplate.opsForHash().get(roomId + ":info", "currentRound"));
        String drawer= null, drawerId = null, currentHiddenWord = null;
        if(gameStatus == GameRoomStatus.DRAWER_SELECTING_WORD || gameStatus == GameRoomStatus.DRAWING){
            drawer = (String)redisTemplate.opsForHash().get(roomId + ":info", "drawer");
            drawerId = (String)redisTemplate.opsForHash().get(roomId + ":info", "drawerId");
        }

        if(gameStatus == GameRoomStatus.DRAWING){
            String currentWord = (String)redisTemplate.opsForHash().get(roomId + ":info", "currentWord");
            currentHiddenWord = currentWord.replaceAll("[^\\s]", "_");
        }

        return new RoomSnapshot(players, host, hostUsername, rounds, timePerRound, chats, gameStatus, phaseDeadLine, currentRound, drawer, drawerId,null, currentHiddenWord);

    }

    public ArrayList<String> getRandomWords(String roomId, Boolean customWordsOnly, Set<String> customWords){
        Set<String> wordsFromRedis = redisTemplate.opsForSet().members(roomId+":currentWords");
        ArrayList<String> listOfCustomWords = new ArrayList<>(customWords);
        ArrayList<String> output = new ArrayList<>();
        if(!wordsFromRedis.isEmpty()){
            int count = 0;
            output.addAll(wordsFromRedis);
            return output;
        }
        for(int i =0;i< 3;i++){
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
        return output;
    }

    public List<String> getWords(Authentication auth){
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String roomId = (String)redisTemplate.opsForValue().get(userDetails.getId()+":room");
        if(roomId==null){
            throw new GameException(HttpStatus.FORBIDDEN, GameExceptionType.USER_NOT_DRAWER, "User not currently in a game room");
        }
        String redisDrawer = (String)redisTemplate.opsForHash().get(roomId + ":info", "drawerId");
        if(!userDetails.getId().equals(redisDrawer)){
            throw new GameException(HttpStatus.FORBIDDEN, GameExceptionType.USER_NOT_DRAWER, "Its not your turn to draw");
        }
        Boolean customWordsOnly = Boolean.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "customWordsOnly"));
        Set<String> customWords = redisTemplate.opsForSet().members(roomId+":customWords");
        return getRandomWords(roomId, customWordsOnly, customWords);
    }

    public void choseWord(String word, Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        System.out.println("choosing word by: "+userDetails.getDisplayUsername());
        String roomId = (String)redisTemplate.opsForValue().get(userDetails.getId()+":room");
        String currentStatus = (String)redisTemplate.opsForHash().get(roomId + ":info", "status");

        // to avoid clash with scheduled event
        if(!Objects.equals(currentStatus, GameRoomStatus.DRAWER_SELECTING_WORD.name())){return;}

        System.out.println(roomId);
        String drawerId = (String)redisTemplate.opsForHash().get(roomId+":info","drawerId");
        System.out.println(drawerId);
        if(!drawerId.equals(userDetails.getId())){
            System.out.println("non Drawer");
            throw new GameException(HttpStatus.FORBIDDEN,GameExceptionType.USER_NOT_DRAWER,"Non drawer cannot chose the word");
        }
        Set<String> wordsFromRedis = redisTemplate.opsForSet().members(roomId+":currentWords");
        System.out.println(wordsFromRedis);
        System.out.println(word);
        if(wordsFromRedis.size()==0 || !wordsFromRedis.contains(word)){
            System.out.println("invalid word");
            throw new GameException(HttpStatus.FORBIDDEN, GameExceptionType.INVALID_WORD, "invalid word submitted");
        }
        System.out.println("setting game status to drawing");
        redisTemplate.opsForHash().put(roomId+":info", "status", GameRoomStatus.DRAWING.name());
        redisTemplate.opsForHash().put(roomId+":info","currentWord", word);
        String hiddenWord = word.replaceAll("[^\\s]", "_");
        System.out.println(hiddenWord);

        Long timePerRound = Long.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "timePerRound" ));
        Long phaseDeadLine = System.currentTimeMillis() + timePerRound * 1000;
        redisTemplate.opsForHash().put(roomId + ":info", "phaseDeadLine", String.valueOf(phaseDeadLine));
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                GameEventDTO.builder()
                        .initiator(
                                new PlayerDTO(
                                        userDetails.getId(),
                                        userDetails.getDisplayUsername(),
                                        null,
                                        null
                                )
                        )
                        .type(GameEventType.PLAYER_DRAWING)
                        .data(Map.of(
                                "word",hiddenWord,
                                "phaseDeadLine",phaseDeadLine
                        ))
                        .build()
        );
    }

    public String getCurrentWord(Authentication auth) {
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        String roomId = (String)redisTemplate.opsForValue().get(userDetails.getId()+":room");
        String drawerId = (String)redisTemplate.opsForHash().get(roomId+":info","drawerId");
        if(!drawerId.equals(userDetails.getId())){
            System.out.println("non Drawer");
            throw new GameException(HttpStatus.FORBIDDEN,GameExceptionType.USER_NOT_DRAWER,"Non drawer get the word");
        }
        return (String)redisTemplate.opsForHash().get(roomId+":info","currentWord");
    }

    public MessageSubmitResponse saveAndPropogateChatMessage(String message, Authentication auth) {
        System.out.println(message);
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        message = message.trim();
        //"propogationmessage" needed because processing with mutate "message"
        String propogationMessage = message;
        message = message.toLowerCase();
        System.out.println("propogation message:"+propogationMessage);
        String roomId = (String)redisTemplate.opsForValue().get(userDetails.getId()+":room");
        if(roomId==null){
            System.out.println("user not part of group");
            throw new GameException(HttpStatus.FORBIDDEN, GameExceptionType.USER_NOT_PART_OF_GROUP, "You are not part of a game");
        }

        // if rank is not null then player has already submitted correct word
        Long rank = redisTemplate.opsForZSet().rank(roomId+":roundPoints",userDetails.getId());

        String currentWord = (String)redisTemplate.opsForHash().get(roomId+":info","currentWord");
        System.out.println("current word: "+currentWord);
        String id = UUID.randomUUID().toString();

        //persisting the message
        redisTemplate.opsForList().leftPush(roomId
                +":chat", objectMapper.writeValueAsString(new GameChatMessage(propogationMessage, userDetails.getDisplayUsername(), id)));
        Long count  = redisTemplate.opsForList().size(roomId+":chat");
        //this bounds the message list length to 100
        if(count == 101){
            redisTemplate.opsForList().rightPop(roomId+":chat");
        }

        String drawerId = (String)redisTemplate.opsForHash().get(roomId+":info","drawerId");
        GameRoomStatus currentState = GameRoomStatus.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","status"));
        System.out.println("currentState: "+currentState);
        boolean correct = Objects.equals(message, currentWord) && currentState==GameRoomStatus.DRAWING;
        //the drawer cannot be null here,
        //corrct boolean check the guess is correct the room state is the correct one also checks if the submitter is the drawer or not
        if(correct && rank == null && !userDetails.getId().equals(drawerId)){
            // set to null because frontend should not get the correct word
            propogationMessage = null;

            //points calculation according to time
            Long phaseDeadLine = Long.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","phaseDeadLine"));
            Long timeRemaining = phaseDeadLine - System.currentTimeMillis();
            timeRemaining = Math.max(0, timeRemaining);
            Long points = timeRemaining/10;
            redisTemplate.opsForZSet().add(roomId+":roundPoints",userDetails.getId(), points);

            // hope the math is correct here
            Long totalMembers = redisTemplate.opsForSet().size(roomId+":members");
            Long exitedMembers = redisTemplate.opsForSet().size(roomId+":exitedMembers");
            Long remainingMembers = totalMembers- exitedMembers;
            Long playerWhoGuessedCorrect = redisTemplate.opsForZSet().size(roomId+":roundPoints");
            // minus one because the drawer cannot submit word, so at most total -1 members can submit correctly
            if(Objects.equals(remainingMembers-1, playerWhoGuessedCorrect)){
                GameRoomStatus status = GameRoomStatus.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","status"));
                if(status == GameRoomStatus.DRAWING){  // so that there are no clashes with the scheduled events
                    //immediate state updation for minimum scheduled event clash
                    //redisTemplate.opsForHash().put(roomId+":info","status", GameRoomStatus.DRAWER_SELECTING_WORD.name());

                    Set<String> membersSet = redisTemplate.opsForSet().members(roomId+":members");
                    Set<String> exitedMembersSet = redisTemplate.opsForSet().members(roomId+":exitedMembers");
                    Integer drawerIndex = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info", "drawerIndex"));
                    System.out.println("drawer index: "+drawerIndex);
                    drawerIndex++;
                    Set<String> newDrawerIdSet = redisTemplate.opsForZSet().range(roomId+":turnOrder",drawerIndex, drawerIndex);

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

                    //storing the current player round points
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

                    //calculating the drawers round points
                    String drawer = (String)redisTemplate.opsForHash().get(roomId+":info","drawer");
                    pointsOutput.add(Map.of("points", totalRoundPoints/2, "username", drawer, "id",drawerId));
                    redisTemplate.opsForZSet().incrementScore(roomId+":leaderboard", drawerId, (Integer)(totalRoundPoints/2));

                    // looping through members to get the new available drawer index , if
                    // reached the end of the list then the round ended
                    String newDrawerId = null;
                    for(String newId: newDrawerIdSet) newDrawerId = newId;
                    System.out.println("new drawer id set: "+newDrawerIdSet);
                    boolean reachedEnd = false;
                    while(exitedMembersSet.contains(newDrawerId)){
                        drawerIndex++;
                        newDrawerIdSet = redisTemplate.opsForZSet().range(roomId+":turnOrder",drawerIndex, drawerIndex);
                        System.out.println("current newDrawer set: "+newDrawerIdSet);
                        if(newDrawerIdSet==null || newDrawerIdSet.size()==0){
                            reachedEnd = true;
                            break;
                        }
                        for(String newId: newDrawerIdSet) newDrawerId = newId;
                    }
                    System.out.println("the end was reached: "+reachedEnd);
                    if(reachedEnd){
                        Integer currentRound = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","currentRound"));
                        Integer totalRounds = Integer.valueOf((String)redisTemplate.opsForHash().get(roomId+":info","rounds"));
                        //check to if the last round ended
                        if(currentRound == totalRounds){
                            phaseDeadLine = (Long)(System.currentTimeMillis()+60_000);
                            redisTemplate.opsForHash().putAll(roomId+":info",Map.of("phaseDeadLine", phaseDeadLine, "status", GameRoomStatus.ENDED.name()));
                            messagingTemplate.convertAndSend(
                                    "/topic/room/" + roomId,
                                    GameEventDTO.builder()
                                            .initiator(null)
                                            .type(GameEventType.GAME_END)
                                            .build()
                            );
                        }else{
                            //finding the next available drawer index, hmm probably should add a check over here if the entire list is empty,
                            //the code that deletes the room if all member leave does exist but i guess its still prone to race conditons
                            currentRound++;
                            redisTemplate.opsForHash().put(roomId+":info","currentRound",currentRound);
                            drawerIndex=0;
                            while(exitedMembersSet.contains(newDrawerId)){
                                drawerIndex++;
                                newDrawerIdSet = redisTemplate.opsForZSet().range(roomId+":turnOrder",drawerIndex, drawerIndex);
                                if(newDrawerIdSet == null || newDrawerIdSet.size()==0){
                                    return new MessageSubmitResponse(currentWord, true);
                                }
                                for(String newId: newDrawerIdSet) newDrawerId = newId;
                            }
                            String newDrawerJson = (String)redisTemplate.opsForHash().get(roomId+":playerMeta", newDrawerId);
                            PlayerDTO newDrawerDTO = objectMapper.readValue(newDrawerJson, PlayerDTO.class);
                            redisTemplate.opsForHash().put(roomId+":info","drawerId", newDrawerId);
                            redisTemplate.opsForHash().put(roomId+":info","drawer", newDrawerDTO.getUsername());
                            redisTemplate.opsForHash().put(roomId+":info","drawerIndex", drawerIndex+"");
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
                            System.out.println("new drawer id:"+newDrawerId);
                            String newDrawerJson = (String)redisTemplate.opsForHash().get(roomId+":playerMeta", newDrawerId);
                            PlayerDTO newDrawerDTO = objectMapper.readValue(newDrawerJson, PlayerDTO.class);
                            redisTemplate.opsForHash().put(roomId+":info","drawerId", newDrawerId);
                            redisTemplate.opsForHash().put(roomId+":info","drawer", newDrawerDTO.getUsername());
                            redisTemplate.opsForHash().put(roomId+":info","drawerIndex", drawerIndex+"");
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
                }
            }
        }

        System.out.println("match:"+correct);
        Map<String,Object> data = new HashMap<>();
        data.put("message", propogationMessage);
        data.put("id", id);
        data.put("correct", correct);
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                GameEventDTO.builder()
                        .initiator(new PlayerDTO(userDetails.getId(),userDetails.getDisplayUsername(), null,null))
                        .type(GameEventType.NEW_MESSAGE)
                        .data(data)
                        .build()
        );
        System.out.println("propogated message");

        // check of any race condition if current word becomes null
        if(currentWord==null || message.length()!=currentWord.length()){
            return new MessageSubmitResponse("",false);
        }


        //building the mask
        StringBuilder mask = new StringBuilder();
        for(int i = 0;i< message.length();i++){
            char curr = currentWord.charAt(i);
            if(curr==' '){
                mask.append(curr);
            }else if(curr==message.charAt(i)){
                mask.append(curr);
            }else{
                mask.append('_');
            }
        }
        System.out.println(mask.toString());
        return new MessageSubmitResponse(mask.toString(), correct);
    }
}


