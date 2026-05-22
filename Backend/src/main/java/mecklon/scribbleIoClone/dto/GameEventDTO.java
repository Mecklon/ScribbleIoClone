package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import mecklon.scribbleIoClone.dto.types.GameEventType;

@Data
@AllArgsConstructor
@Builder
public class GameEventDTO {
    PlayerDTO initiator;
    GameEventType type;
}
