package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlayerWithPoints {
    private String id;
    private String username;
    private String profile;
    private Double points;
}
