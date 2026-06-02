package mecklon.scribbleIoClone.controller;


import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.dto.*;
import mecklon.scribbleIoClone.model.User;
import mecklon.scribbleIoClone.repository.UserRepository;
import mecklon.scribbleIoClone.service.CustomUserDetails;
import mecklon.scribbleIoClone.service.GameService;
import mecklon.scribbleIoClone.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final GameService gameService;

    @PostMapping("/autoLogin")
    public ResponseEntity<AuthResponse> autoLogin(Authentication auth){
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(user.getId(),null, userDetails.getUsername(), userDetails.getDisplayUsername(), user.getFileName()));
    }



    @PostMapping("/updateProfile")
    ResponseEntity<ProfileDTO> updateProfile(@RequestParam(name="username", required = false) String username, @RequestParam(name = "profile", required = false) MultipartFile profile, Authentication auth){
       try{
           return ResponseEntity.status(HttpStatus.OK).body(userService.updateProfile(profile, username, auth));
       }catch (IOException e){
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
       }
    }

    @GetMapping("/createRoom")
    ResponseEntity<String> createRoom(Authentication auth){
        return ResponseEntity.status(HttpStatus.OK).body(gameService.createRoom(auth));
    }

    @GetMapping("/joinRoom/{roomId}")
    ResponseEntity<RoomDetails> joinRoom(Authentication auth, @PathVariable("roomId") String roomId){
        return ResponseEntity.status(HttpStatus.OK).body(gameService.joinRoom(auth, roomId));
    }

    @PostMapping("/test/{input}")
    public ResponseEntity<Void> canthitthis(@PathVariable("input") String input){
        System.out.println(input);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @PostMapping("/startGame")
    public ResponseEntity<Void> startGame(@RequestBody GameStartRequest request, Authentication auth){
        gameService.startGame(request, auth);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/joinGame/{roomId}")
    public ResponseEntity<RoomSnapshot> joinGame(@PathVariable("roomId") String roomId, Authentication auth){
        return ResponseEntity.status(HttpStatus.OK).body(gameService.joinGame(roomId, auth));
    }

    @GetMapping("/getRandomWords")
    public ResponseEntity<List<String>> getRandomWords(Authentication auth){
        return ResponseEntity.status(HttpStatus.OK).body(gameService.getWords(auth));
    }

    @PostMapping("/chooseWord")
    public ResponseEntity<Void> chooseWord(@RequestBody Map<String,String> body, Authentication auth){
        gameService.choseWord(body.get("word"), auth);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/getCurrentWord")
    public ResponseEntity<String> getCurrentWord(Authentication auth){
        return ResponseEntity.status(HttpStatus.OK).body(gameService.getCurrentWord(auth));
    }


    @PostMapping("/chatInput")
    public ResponseEntity<MessageSubmitResponse> saveAndPropogateChatMessage(@RequestBody Map<String,String> body, Authentication auth){
        return ResponseEntity.status(HttpStatus.OK).body(gameService.saveAndPropogateChatMessage(body.get("message"), auth));
    }

    @PostMapping("/sendCanvasEvents")
    public ResponseEntity<Void> saveAndPropogateCanvasEvents(@RequestBody List<CanvasEvent> events, Authentication auth){
        gameService.saveAndPropogateCanvasEvents(events, auth);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

}
