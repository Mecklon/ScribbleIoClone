package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SettingsChangeDTO {
    private Integer rounds;
    private Integer timePerRound;
    private String roomId;
}
