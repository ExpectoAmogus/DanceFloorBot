package com.discord.DanceFloorBot.other;

import com.sedmelluq.discord.lavaplayer.filter.equalizer.EqualizerFactory;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import discord4j.common.util.Snowflake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.discord.DanceFloorBot.service.AudioManager.PLAYER_MANAGER;

public final class GuildAudioManager {
    public static final float[] BASS_BOOST =
            {0.2f, 0.15f, 0.1f, 0.05f, 0.0f, -0.05f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f, -0.1f};
    private static final Map<Snowflake, GuildAudioManager> MANAGERS = new ConcurrentHashMap<>();
    private final AudioPlayer player;
    private final AudioTrackScheduler scheduler;
    private final LavaPlayerAudioProvider provider;
    private final EqualizerFactory equalizer;

    private GuildAudioManager() {
        player = PLAYER_MANAGER.createPlayer();
        scheduler = new AudioTrackScheduler(player);
        provider = new LavaPlayerAudioProvider(player);
        equalizer = new EqualizerFactory();

        player.addListener(scheduler);
        player.setFilterFactory(equalizer);
        player.setFrameBufferDuration(500);
    }

    public static GuildAudioManager of(final Snowflake id) {
        return MANAGERS.computeIfAbsent(id, ignored -> new GuildAudioManager());
    }

    public AudioPlayer getPlayer() {
        return player;
    }

    public AudioTrackScheduler getScheduler() {
        return scheduler;
    }

    public LavaPlayerAudioProvider getProvider() {
        return provider;
    }

    public void bassBoost(float percentage) {
        final float multiplier = percentage / 100.00f;

        for (int i = 0; i < 4; i++) {
            equalizer.setGain(i, BASS_BOOST[i] * multiplier);
        }
    }
}
