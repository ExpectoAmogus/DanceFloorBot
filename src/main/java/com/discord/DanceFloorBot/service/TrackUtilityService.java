package com.discord.DanceFloorBot.service;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class TrackUtilityService {
    public String getTrackDuration(Optional<AudioTrack> currentTrack) {
        String trackDuration = "";
        if (currentTrack.isPresent()) {
            long durationInMillis = currentTrack.get().getDuration();
            long positionInMillis= currentTrack.get().getPosition();
            Duration duration = Duration.ofMillis(durationInMillis - positionInMillis);
            long seconds = duration.getSeconds();
            long absSeconds = Math.abs(seconds);
            trackDuration = String.format("%02d:%02d", absSeconds / 60, absSeconds % 60);
        }
        return trackDuration;
    }
}
