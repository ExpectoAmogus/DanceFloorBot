package com.discord.DanceFloorBot.commands;

import com.discord.DanceFloorBot.handlers.CommandMethodsHandler;
import com.discord.DanceFloorBot.handlers.MessageBuilder;
import com.discord.DanceFloorBot.other.AudioTrackScheduler;
import com.discord.DanceFloorBot.other.GuildAudioManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class ReshuffleCommand implements SlashCommand {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();

    @Override
    public String getName() {
        return "reshuffle";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final VoiceChannel channel = commandMethodsHandler.getVoiceChannel(event);
        final AudioTrackScheduler audioTrackScheduler = GuildAudioManager.of(channel.getGuildId()).getScheduler();
        Optional<List<AudioTrack>> audioTracks = audioTrackScheduler.getQueue();
        if (audioTracks.isPresent()) {
            Collections.shuffle(audioTracks.get());
            event.reply()
                    .withEphemeral(false)
                    .withEmbeds(messageBuilder.getBlankBuilder("Playlist has been shuffled"))
                    .subscribe();
        } else {
            event.reply()
                    .withEphemeral(true)
                    .withEmbeds(messageBuilder.getBlankBuilder("Queue is empty"))
                    .subscribe();
        }
        return Mono.empty();
    }
}

