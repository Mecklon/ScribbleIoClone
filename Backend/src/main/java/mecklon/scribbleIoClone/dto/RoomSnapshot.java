package mecklon.scribbleIoClone.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import mecklon.scribbleIoClone.service.GameRoomStatus;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class RoomSnapshot {
    private List<GamePlayerDTO> players;
    private String host;
    private String hostUsername;
    private Integer rounds;
    private Integer timePerRound;
    private List<GameChatMessage> chats;
    private GameRoomStatus status;
    private Long phaseDeadLine;
    private Integer currentRound;
    private String drawer;
    private String drawerId;
    private String currentWord;
    private String currentHiddenWord;
    private List<CanvasEvent> canvasEvents;
}
