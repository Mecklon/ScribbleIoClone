package mecklon.scribbleIoClone.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "game")
@Data
public class GameProperties {

    private long drawerSelectTime = 20;
    private long roundEndTime = 5;
    private long pointsDisplayTime = 7;
    private long lobbyGameSwitchTime = 20;
}