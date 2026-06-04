package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GamePlayerDTO {
    private PlayerDTO player;
    private Integer points;
    private Boolean connected;
    private Integer rank;
}
