package mecklon.scribbleIoClone.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GamePropertiesService {
    private final GameProperties gameProperties;

    public Long getDrawerSelectDuration(){
        return gameProperties.getDrawerSelectTime()*1000;
    }
    public Long getRoundEndDuration(){
        return gameProperties.getRoundEndTime()*1000;
    }
    public Long getPointsDisplayDuration(){
        return gameProperties.getPointsDisplayTime()*1000;
    }
    public Long getLobbySwitchDeadline(){
        return gameProperties.getLobbyGameSwitchTime()*1000 + System.currentTimeMillis();
    }

    public Long getDrawerSelectDeadline(){
        return getPointsDisplayDuration() + getDrawerSelectDuration() + System.currentTimeMillis();
    }

    public Long getRoundEndDeadline(){
        return getPointsDisplayDuration() + getDrawerSelectDuration() + getRoundEndDuration() + System.currentTimeMillis();
    }

    public Long GameDropDeadline(){
        return System.currentTimeMillis() + 60_000;
    }

}


