package mecklon.scribbleIoClone.controller;

import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.Exceptions.UserAlreadyExistsException;
import mecklon.scribbleIoClone.configuration.JwtUtil;
import mecklon.scribbleIoClone.dto.AuthRequest;
import mecklon.scribbleIoClone.dto.AuthResponse;
import mecklon.scribbleIoClone.dto.AutoLoginRequest;
import mecklon.scribbleIoClone.dto.SignupRequest;
import mecklon.scribbleIoClone.model.User;
import mecklon.scribbleIoClone.repository.UserRepository;
import mecklon.scribbleIoClone.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService
            .loadUserByUsername(request.getEmail());

        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(token, request.getEmail() );
    }


    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request){
        String token = jwtUtil.generateToken(request.email);

        if(request.username==null || request.username.isEmpty() ||
                request.email==null || request.email.isEmpty() ||
                request.password==null || request.password.isEmpty()){
            throw new BadCredentialsException("email or password not found");
        }
        User user = userRepository.findByEmail(request.getEmail());
        if(user!=null){
            throw new UserAlreadyExistsException("User with this email already exists");
        }
        user = new User(request.email, passwordEncoder.encode(request.password), request.username);
        userRepository.save(user);


        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, request.email));
    }



    @Autowired
    private org.springframework.core.env.Environment env;

    @GetMapping("/db")
    public String db() {
        return "database = " + env.getProperty("spring.data.mongodb.database")
                + "\nuri = " + env.getProperty("spring.data.mongodb.uri");
    }

}
