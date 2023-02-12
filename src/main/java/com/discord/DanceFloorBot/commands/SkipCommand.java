package com.discord.DanceFloorBot.commands;

import com.discord.DanceFloorBot.handlers.CommandMethodsHandler;
import com.discord.DanceFloorBot.handlers.MessageBuilder;
import com.discord.DanceFloorBot.other.AudioTrackScheduler;
import com.discord.DanceFloorBot.other.GuildAudioManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;


@Component
public class SkipCommand implements SlashCommand {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();

    @Override
    public String getName() {
        return "skip";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final VoiceChannel voiceChannel = commandMethodsHandler.getVoiceChannel(event);
        final AudioTrackScheduler audioTrackScheduler = GuildAudioManager.of(voiceChannel.getGuildId()).getScheduler();
        final Optional<AudioTrack> currentTrack = audioTrackScheduler.getCurrentTrack();
        Optional<List<AudioTrack>> audioTracks = audioTrackScheduler.getQueue();
        final Long L_number = event.getOption("number")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .orElse(null);
        if (L_number == null) {
            if (currentTrack.isPresent()) {
                audioTracks.ifPresent(tracks -> tracks.remove(currentTrack.get()));
                audioTrackScheduler.restart();
                audioTrackScheduler.setLooped(false);
                audioTrackScheduler.skip();
                event.reply()
                        .withEphemeral(true)
                        .withEmbeds(messageBuilder.getBlankBuilderWithTrack("Skipped", currentTrack.get()))
                        .subscribe();
            } else {
                event.reply()
                        .withEphemeral(true)
                        .withEmbeds(messageBuilder.getBlankBuilder("I don't have a track playing"))
                        .subscribe();
            }
        }
        else if (audioTracks.isPresent() && L_number < audioTracks.get().size()){
            event.reply()
                    .withEphemeral(true)
                    .withEmbeds(messageBuilder.getBlankBuilderWithTrack("Skipped", audioTracks.get().remove(Math.toIntExact(L_number) - 1)))
                    .subscribe();
        }
        else {
            event.reply()
                    .withEphemeral(true)
                    .withEmbeds(messageBuilder.getBlankBuilder("You provide a wrong number"))
                    .subscribe();
        }

        return Mono.empty();
    }
}
