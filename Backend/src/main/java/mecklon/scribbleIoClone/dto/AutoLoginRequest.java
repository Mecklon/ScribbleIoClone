package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
public class AutoLoginRequest {
    private String token;
}
