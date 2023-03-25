package com.discord.DanceFloorBot.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DanceFloorBotConfig {

    private static final Logger log = LoggerFactory.getLogger(DanceFloorBotConfig.class);

    @Bean
    public GatewayDiscordClient gatewayDiscordClient() {
        GatewayDiscordClient client = null;
        try {
            client = DiscordClientBuilder.create(System.getenv("token"))
                    .build()
                    .gateway()
                    .setEnabledIntents(IntentSet.of(Intent.GUILD_INTEGRATIONS, Intent.GUILD_MESSAGES,Intent.GUILD_VOICE_STATES, Intent.GUILDS, Intent.GUILD_EMOJIS_AND_STICKERS,Intent.GUILD_MESSAGE_REACTIONS, Intent.GUILD_MESSAGE_TYPING))
                    .setInitialPresence(ignore -> ClientPresence
                            .online(ClientActivity.listening(" /play")))
                    .login()
                    .block();
        } catch (Exception exception) {
            log.error("Be sure to use a valid bot token!", exception);
        }
        return client;
    }

    @Bean
    public RestClient discordRestClient(GatewayDiscordClient client) {
        return client.getRestClient();
    }
}
