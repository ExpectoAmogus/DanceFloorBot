package com.discord.DanceFloorBot.handlers;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;

import java.util.Optional;

public class CommandMethodsHandler {

    public CommandMethodsHandler() {}

    public VoiceChannel getVoiceChannel(ChatInputInteractionEvent event) {
        final Optional<Snowflake> channelId = event.getInteraction()
                .getMember().get()
                .getVoiceState().block()
                .getChannelId();
        return (VoiceChannel) event.getClient().getChannelById(channelId.get()).block();
    }

    public TextChannel getTextChannel(ChatInputInteractionEvent event) {
        final Optional<Snowflake> channelId = Optional.of(event.getInteraction().getChannelId());
        return (TextChannel) event.getClient().getChannelById(channelId.get()).block();
    }

}
