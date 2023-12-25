package com.discord.DanceFloorBot.service;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import org.springframework.stereotype.Service;

@Service
public class AudioManager {
    public static final AudioPlayerManager PLAYER_MANAGER;

    static {
        PLAYER_MANAGER = new DefaultAudioPlayerManager();
        // This is an optimization strategy that Discord4J can utilize to minimize allocations
        PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        PLAYER_MANAGER.getConfiguration().setFilterHotSwapEnabled(true);
//        PLAYER_MANAGER.registerSourceManager(new YoutubeAudioSourceManager(true, System.getenv("google_email"), System.getenv("google_pass")));
        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);
    }
}
