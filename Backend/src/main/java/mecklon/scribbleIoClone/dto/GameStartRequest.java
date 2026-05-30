package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameStartRequest {
    private String roomId;
    private Integer timePerRound;
    private Integer rounds;
    private String customWords;
    private Boolean onlyCustomWords;
}
