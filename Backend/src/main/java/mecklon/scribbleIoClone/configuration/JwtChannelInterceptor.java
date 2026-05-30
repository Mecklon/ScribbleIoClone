package mecklon.scribbleIoClone.configuration;

import mecklon.scribbleIoClone.service.CustomUserDetails;
import mecklon.scribbleIoClone.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token =
                    accessor.getFirstNativeHeader(
                            "Authorization");

            if (token != null &&
                    token.startsWith("Bearer ")) {

                String jwt = token.substring(7);

                if (jwtUtil.validateToken(jwt)) {

                    String username =
                            jwtUtil.extractUsername(jwt);

                    CustomUserDetails userDetails =
                            (CustomUserDetails)
                                    customUserDetailsService
                                            .loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    accessor.setUser(auth);
                    accessor.getSessionAttributes()
                            .put("auth", auth);
                }
            }
        } else {

            Authentication auth =
                    (Authentication)
                            accessor.getSessionAttributes()
                                    .get("auth");
            if (auth != null) {
                accessor.setUser(auth);
            }
        }
        return message;
    }
}