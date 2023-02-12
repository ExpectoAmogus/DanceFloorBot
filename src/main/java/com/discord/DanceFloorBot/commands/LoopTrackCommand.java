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

import java.util.Optional;

@Component
public class LoopTrackCommand implements SlashCommand {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();

    @Override
    public String getName() {
        return "loop_track";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final VoiceChannel voiceChannel = commandMethodsHandler.getVoiceChannel(event);
        final AudioTrackScheduler audioTrackScheduler = GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler();
        final Optional<AudioTrack> currentTrack = audioTrackScheduler.getCurrentTrack();
        if (currentTrack.isPresent()) {
            if (!audioTrackScheduler.isLooped()) {
                audioTrackScheduler.setLooped(true);
                event.reply()
                        .withEphemeral(false)
                        .withEmbeds(messageBuilder.getBlankBuilderWithTrack(
                                "Loop the current track", currentTrack.get()))
                        .subscribe();
            } else {
                audioTrackScheduler.setLooped(false);
                event.reply()
                        .withEphemeral(false)
                        .withEmbeds(messageBuilder.getBlankBuilderWithTrack(
                                "Unloop the current track", currentTrack.get()))
                        .subscribe();
            }
        } else {
            event.reply()
                    .withEphemeral(true)
                    .withEmbeds(messageBuilder.getBlankBuilder(
                            "You didn't request any track"))
                    .subscribe();
        }

        return Mono.empty();
    }
}
