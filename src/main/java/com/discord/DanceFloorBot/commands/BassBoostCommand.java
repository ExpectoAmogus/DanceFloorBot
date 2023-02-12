package com.discord.DanceFloorBot.commands;

import com.discord.DanceFloorBot.handlers.CommandMethodsHandler;
import com.discord.DanceFloorBot.handlers.MessageBuilder;
import com.discord.DanceFloorBot.other.GuildAudioManager;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class BassBoostCommand implements SlashCommand {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();

    @Override
    public String getName() {
        return "bass_boost";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final VoiceChannel channel = commandMethodsHandler.getVoiceChannel(event);
        float boostF = event.getOption("value")
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .get();
        GuildAudioManager.of(channel.getGuildId()).bassBoost(boostF);
        event.reply()
                .withEphemeral(true)
                .withEmbeds(messageBuilder.getBlankBuilder("Changing bass boost for " + boostF + "%"))
                .subscribe();
        return Mono.empty();
    }
}
