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

import java.util.List;
import java.util.Optional;

@Component
public class ClearCommand implements SlashCommand {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();

    @Override
    public String getName() {
        return "clear";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final VoiceChannel channel = commandMethodsHandler.getVoiceChannel(event);
        final AudioTrackScheduler audioTrackScheduler = GuildAudioManager.of(channel.getGuildId()).getScheduler();
        Optional<List<AudioTrack>> queue = audioTrackScheduler.getQueue();
        audioTrackScheduler.stop();
        audioTrackScheduler.setLooped(false);
        audioTrackScheduler.setShuffle(false);
        if (queue.isPresent()) {
            queue.get().clear();
            event.reply()
                    .withEphemeral(false)
                    .withEmbeds(messageBuilder.getBlankBuilder("Queue has been cleared"))
                    .subscribe();
        }
        else {
            event.reply()
                    .withEphemeral(true)
                    .withEmbeds(messageBuilder.getBlankBuilder("Queue has already empty"))
                    .subscribe();

        }
        return Mono.empty();
    }
}

