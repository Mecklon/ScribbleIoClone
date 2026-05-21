package mecklon.scribbleIoClone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

@Data
@AllArgsConstructor
public class AuthRequest {
    private String email;
    private String password;
}