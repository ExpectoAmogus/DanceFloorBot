package com.discord.DanceFloorBot.commands;

import com.discord.DanceFloorBot.handlers.CommandMethodsHandler;
import com.discord.DanceFloorBot.handlers.MessageBuilder;
import com.discord.DanceFloorBot.other.AudioTrackScheduler;
import com.discord.DanceFloorBot.other.GuildAudioManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
public class StopCommand implements SlashCommand {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();

    @Override
    public String getName() {
        return "stop";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final VoiceChannel channel = commandMethodsHandler.getVoiceChannel(event);
        final AudioTrackScheduler audioTrackScheduler = GuildAudioManager.of(channel.getGuildId()).getScheduler();
        final AudioProvider provider = GuildAudioManager.of(channel.getGuildId()).getProvider();

        final Mono<Void> onDisconnect = channel.join(spec -> spec.setProvider(provider))
                .flatMap(connection -> Mono.firstWithSignal(connection.disconnect()));

        Optional<List<AudioTrack>> queue = audioTrackScheduler.getQueue();
        audioTrackScheduler.stop();
        audioTrackScheduler.setLooped(false);
        queue.ifPresent(List::clear);
        event.reply()
                .withEphemeral(true)
                .withEmbeds(messageBuilder.getBlankBuilder("Bye bye 0/"))
                .subscribe();
        return Mono.firstWithSignal(onDisconnect);
    }
}

