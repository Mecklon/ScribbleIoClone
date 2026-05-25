package mecklon.scribbleIoClone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String id;
    private String token;
    private String email;
    private String username;
    private String profile;
}