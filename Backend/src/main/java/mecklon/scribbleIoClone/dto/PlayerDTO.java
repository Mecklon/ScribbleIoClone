package mecklon.scribbleIoClone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerDTO {
    private String username;
    private String email;
    private String profile;
}
