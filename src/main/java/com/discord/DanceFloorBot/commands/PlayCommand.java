package com.discord.DanceFloorBot.commands;

import com.discord.DanceFloorBot.handlers.CommandMethodsHandler;
import com.discord.DanceFloorBot.handlers.MessageBuilder;
import com.discord.DanceFloorBot.other.AudioTrackScheduler;
import com.discord.DanceFloorBot.other.GuildAudioManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.VoiceState;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

import static com.discord.DanceFloorBot.service.AudioManager.PLAYER_MANAGER;

@Component
public class PlayCommand implements SlashCommand {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();

    private static Mono<Void> getDisconnect(VoiceChannel channel, AudioProvider provider) {
        return channel.join(spec -> spec.setProvider(provider))
                .flatMap(connection -> {

                    // The bot itself has a VoiceState; 1 VoiceState signals bot is alone
                    final Publisher<Boolean> voiceStateCounter = channel.getVoiceStates()
                            .count()
                            .map(count -> 1L == count)
                            .single();

                    // After 10 seconds, check if the bot is alone. This is useful if
                    // the bot joined alone, but no one else joined since connecting
                    final Mono<Void> onDelay = Mono.delay(Duration.ofSeconds(10L))
                            .filterWhen(ignored -> voiceStateCounter)
                            .switchIfEmpty(Mono.never())
                            .then();

                    // As people join and leave `channel`, check if the bot is alone.
                    // Note the first filter is not strictly necessary, but it does prevent many unnecessary cache calls
                    final Mono<Void> onEvent = channel.getClient().getEventDispatcher().on(VoiceStateUpdateEvent.class)
                            .filter(event1 -> event1.getOld().flatMap(VoiceState::getChannelId).map(channel.getId()::equals).orElse(false))
                            .filterWhen(ignored -> voiceStateCounter)
                            .take(1)
                            .then();
                    // Disconnect the bot if either onDelay or onEvent are completed!
                    return Mono.firstWithSignal(onEvent, onDelay)
                            .then(connection.disconnect());
                });
    }

    @Override
    public String getName() {
        return "play";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        final VoiceChannel channel = commandMethodsHandler.getVoiceChannel(event);
        final AudioProvider provider = GuildAudioManager.of(channel.getGuildId()).getProvider();
        final AudioTrackScheduler audioTrackScheduler = GuildAudioManager.of(channel.getGuildId()).getScheduler();
        audioTrackScheduler.setEvent(event);
        final Mono<Void> onDisconnect = getDisconnect(channel, provider);
        return channel.join(spec -> spec.setProvider(provider))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(connection -> {
                    final String trackUrl = event.getOption("query")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asString)
                            .orElse(null);
                    final boolean priority = event.getOption("priority")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asBoolean)
                            .orElse(false);
                    final Long L_number = event.getOption("number")
                            .flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asLong)
                            .orElse(null);
                    audioTrackScheduler.setPriority(priority);
                    if (L_number != null && trackUrl == null) {
                        int number = Math.toIntExact(L_number);
                        if (audioTrackScheduler.getQueue().isPresent()) {
                            if (number < audioTrackScheduler.getQueue().get().size()) {
                                audioTrackScheduler.stop();
                                AudioTrack track = audioTrackScheduler.getQueue().get().remove(number - 1);
                                audioTrackScheduler.play(track);
                                event.reply()
                                        .withEphemeral(true)
                                        .withEmbeds(messageBuilder.getBlankBuilder("Ok"))
                                        .then();
                            } else {
                                event.reply()
                                        .withEphemeral(true)
                                        .withEmbeds(messageBuilder.getBlankBuilder("You provide wrong number"))
                                        .then();
                            }
                        } else {
                            event.reply()
                                    .withEphemeral(true)
                                    .withEmbeds(messageBuilder.getBlankBuilder("The queue is empty"))
                                    .then();
                        }
                    } else if (L_number == null && trackUrl != null) {
                        PLAYER_MANAGER.loadItem(trackUrl, new AudioLoadResultHandler() {
                            @Override
                            public void trackLoaded(AudioTrack track) {
                                audioTrackScheduler.play(track);
                                event.reply()
                                        .withEphemeral(true)
                                        .withEmbeds(messageBuilder.getBlankBuilderWithTrack(
                                                "Track added to the queue", track))
                                        .subscribe();
                            }

                            @Override
                            public void playlistLoaded(AudioPlaylist playlist) {
                                if (!audioTrackScheduler.isPriority()) {
                                    for (int i = 0; i < playlist.getTracks().size(); i++) {
                                        audioTrackScheduler.getQueue().get().add(playlist.getTracks().get(i));
                                    }
                                } else {
                                    for (int i = playlist.getTracks().size() - 1; i >= 0; i--) {
                                        audioTrackScheduler.getQueue().get().add(0, playlist.getTracks().get(i));
                                    }
                                }
                                event.reply()
                                        .withEphemeral(true)
                                        .withEmbeds(messageBuilder.getBlankBuilderWithPlayList(
                                                "Playlist added to the queue", playlist))
                                        .subscribe();
                            }

                            @Override
                            public void noMatches() {
                                event.reply()
                                        .withEphemeral(true)
                                        .withEmbeds(messageBuilder.getBlankBuilder(
                                                "No tracks found with the URL" + trackUrl))
                                        .subscribe();
                            }

                            @Override
                            public void loadFailed(FriendlyException exception) {
                                event.reply()
                                        .withEphemeral(true)
                                        .withEmbeds(messageBuilder.getBlankBuilder(
                                                "Failed to load track" + exception.getMessage()))
                                        .subscribe();
                            }
                        });
                    } else if (L_number == null && !priority) {
                        if (audioTrackScheduler.getQueue().get().size() > 1) {
                            audioTrackScheduler.playNext();
                            event.reply()
                                    .withEphemeral(true)
                                    .withEmbeds(messageBuilder.getBlankBuilder("Ok"))
                                    .subscribe();
                        } else {
                            event.reply()
                                    .withEphemeral(true)
                                    .withEmbeds(messageBuilder.getBlankBuilder("Hi!"))
                                    .subscribe();
                        }

                    } else {
                        event.reply()
                                .withEphemeral(true)
                                .withEmbeds(messageBuilder.getBlankBuilder(
                                        "You must provide either a track URL or a number"))
                                .subscribe();
                    }
                    return Mono.firstWithSignal(onDisconnect);
                });
    }
}
