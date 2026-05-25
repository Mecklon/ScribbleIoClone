package mecklon.scribbleIoClone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoomDetails {
    List<PlayerDTO> players;
    String host;
    Integer rounds;
    Integer timePerRound;
    String hostUsername;
}
