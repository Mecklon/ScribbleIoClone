package mecklon.scribbleIoClone.controller;


import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.dto.AuthResponse;
import mecklon.scribbleIoClone.dto.AutoLoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.net.http.HttpResponse;

@Controller
@RequiredArgsConstructor
public class LobbyController {

    @PostMapping("/autoLogin")
    public ResponseEntity<String> autoLogin(Authentication auth){
        UserDetails user = (UserDetails)auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body("hello world"+ user.getPassword());
    }

    @PostMapping("/test/{input}")
    public ResponseEntity<Void> canthitthis(@PathVariable("input") String input){
        System.out.println(input);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
