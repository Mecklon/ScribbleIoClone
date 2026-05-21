package mecklon.scribbleIoClone.controller;


import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.dto.AuthResponse;
import mecklon.scribbleIoClone.dto.AutoLoginRequest;
import mecklon.scribbleIoClone.dto.ProfileDTO;
import mecklon.scribbleIoClone.model.User;
import mecklon.scribbleIoClone.repository.UserRepository;
import mecklon.scribbleIoClone.service.CustomUserDetails;
import mecklon.scribbleIoClone.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class LobbyController {

    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/autoLogin")
    public ResponseEntity<AuthResponse> autoLogin(Authentication auth){
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
        User u = userRepository.findByEmail(user.getUsername());
        return ResponseEntity.status(HttpStatus.OK).body(new AuthResponse(null, user.getUsername(), user.getDisplayUsername(), u.getFileName()));
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
    ResponseEntity<Void> createRoom(Authentication auth){
        return null;
    }

    @PostMapping("/test/{input}")
    public ResponseEntity<Void> canthitthis(@PathVariable("input") String input){
        System.out.println(input);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
