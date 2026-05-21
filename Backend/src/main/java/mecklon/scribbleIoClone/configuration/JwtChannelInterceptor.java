package mecklon.scribbleIoClone.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor
        implements ChannelInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(
                accessor.getCommand())) {

            String token =
                    accessor.getFirstNativeHeader(
                            "Authorization");

            if (token != null &&
                    token.startsWith("Bearer ")) {

                String jwt = token.substring(7);

                if (jwtUtil.validateToken(jwt)) {
                    String username =
                            jwtUtil.extractUsername(jwt);

                    accessor.setUser(() -> username);

                    accessor.getSessionAttributes()
                            .put("username", username);
                }
            }
        } else if (accessor.getUser() == null) {
            String username =
                    (String) accessor
                            .getSessionAttributes()
                            .get("username");

            if (username != null) {
                accessor.setUser(() -> username);
            }
        }

        return message;
    }
}