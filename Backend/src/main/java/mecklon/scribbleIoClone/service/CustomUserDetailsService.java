package mecklon.scribbleIoClone.service;


import lombok.RequiredArgsConstructor;
import mecklon.scribbleIoClone.model.User;
import mecklon.scribbleIoClone.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    public final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email);
        if(user==null){
            throw new UsernameNotFoundException("email not found in the database");
        }
        return org.springframework.security.core.userdetails.User
                .withUsername(String.valueOf(user.getEmail()))
                .password(user.getPassword())
                .build();
    }
}