package com.discord.DanceFloorBot.commands;

import com.discord.DanceFloorBot.handlers.CommandMethodsHandler;
import com.discord.DanceFloorBot.handlers.MessageBuilder;
import com.discord.DanceFloorBot.other.AudioTrackScheduler;
import com.discord.DanceFloorBot.other.GuildAudioManager;
import com.discord.DanceFloorBot.service.TrackUtilityService;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateFields;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class PlaylistCommand implements SlashCommand {
    private final TrackUtilityService trackUtilityService;
    private final MessageBuilder messageBuilder = new MessageBuilder();
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private int page = 1;
    private int totalPages;
    private List<EmbedCreateFields.Field> fields;
    private TextChannel textChannel;
    private List<AudioTrack> audioTracks;

    private Mono<Void> onClick(Optional<AudioTrack> currentTrack, AudioTrackScheduler audioTrackScheduler) {
        return textChannel.getClient().getEventDispatcher().on(ButtonInteractionEvent.class, buttonEvent -> {
                    switch (buttonEvent.getCustomId()) {
                        case "prev-button" -> {
                            if (page <= 1) {
                                //need to disable prev button when first page loaded

                            } else {
                                page--;
                                changePage();
                            }
                            String trackDuration = trackUtilityService.getTrackDuration(currentTrack);
                            return currentTrack.map(track -> buttonEvent.edit()
                                    .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", ">\t" + track.getInfo().title + "\t" + trackDuration, fields, page, totalPages, audioTrackScheduler.isShuffle()))).orElseGet(() -> buttonEvent.edit()
                                    .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", "", fields, page, totalPages, audioTrackScheduler.isShuffle())));
                        }
                        case "next-button" -> {
                            if (page >= totalPages) {
                                //need to disable next button when last page loaded
                            } else {
                                page++;
                                changePage();
                            }
                            String trackDuration = trackUtilityService.getTrackDuration(currentTrack);
                            return currentTrack.map(track -> buttonEvent.edit()
                                    .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", ">\t" + track.getInfo().title + "\t" + trackDuration, fields, page, totalPages, audioTrackScheduler.isShuffle()))).orElseGet(() -> buttonEvent.edit()
                                    .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", "", fields, page, totalPages, audioTrackScheduler.isShuffle())));
                        }
                        case "update-button" -> {
                            changePage();
                            String trackDuration = trackUtilityService.getTrackDuration(currentTrack);
                            return currentTrack.map(track -> buttonEvent.edit()
                                    .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", ">\t" + track.getInfo().title + "\t" + trackDuration, fields, page, totalPages, audioTrackScheduler.isShuffle()))).orElseGet(() -> buttonEvent.edit()
                                    .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", "", fields, page, totalPages, audioTrackScheduler.isShuffle())));
                        }
                    }

                    return Mono.empty();
                })
                .timeout(Duration.ofMinutes(2))
                //after timeout should update message and disable all buttons in it
                .onErrorResume(TimeoutException.class, ignore -> Mono.empty())
                .then();
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        page = 1;
        final VoiceChannel channel = commandMethodsHandler.getVoiceChannel(event);
        textChannel = commandMethodsHandler.getTextChannel(event);
        final AudioTrackScheduler audioTrackScheduler = GuildAudioManager.of(channel.getGuildId()).getScheduler();
        final Optional<AudioTrack> currentTrack = audioTrackScheduler.getCurrentTrack();
        Optional<List<AudioTrack>> audioTracks = audioTrackScheduler.getQueue();

        if (audioTracks.isPresent()) {
            this.audioTracks = audioTracks.get();
            totalPages = this.audioTracks.size() / 10 + (this.audioTracks.size() % 10 == 0 ? 0 : 1);
            changePage();

            Button prev = Button.primary("prev-button", "<-");
            Button next = Button.primary("next-button", "->");
            String updateLabel = "⟳";
            Button update = Button.secondary("update-button", updateLabel);

            if (currentTrack.isPresent()) {
                long durationInMillis = currentTrack.get().getDuration();
                Duration duration = Duration.ofMillis(durationInMillis);

                long seconds = duration.getSeconds();
                long absSeconds = Math.abs(seconds);
                String formattedDuration = String.format("%02d:%02d",
                        absSeconds / 60, absSeconds % 60);

                if (seconds < 0) {
                    formattedDuration = "-" + formattedDuration;
                }
                event.reply()
                        .withEphemeral(false)
                        .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", ">\t" + currentTrack.get().getInfo().title + "\t" + formattedDuration, fields, page, totalPages, audioTrackScheduler.isShuffle()))
                        .withComponents(ActionRow.of(prev, next, update))
                        .subscribe();
            } else {
                event.reply()
                        .withEphemeral(false)
                        .withEmbeds(messageBuilder.getPlayListBuilder("Current playlist", "", fields, page, totalPages, audioTrackScheduler.isShuffle()))
                        .withComponents(ActionRow.of(prev, next, update))
                        .subscribe();
            }

        } else {
            event.reply()
                    .withEphemeral(true)
                    .withEmbeds(messageBuilder.getBlankBuilder("Queue is empty"))
                    .subscribe();
        }
        return Mono.firstWithSignal(onClick(currentTrack, audioTrackScheduler));
    }

    private void changePage() {
        fields = new ArrayList<>();
        int start = (page - 1) * 10;
        int end = Math.min(page * 10, this.audioTracks.size());
        for (int i = start; i < end; i++) {
            AudioTrack audioTrack = this.audioTracks.get(i);
            String trackDuration = trackUtilityService.getTrackDuration(Optional.ofNullable(audioTrack));
            int finalI = i;
            fields.add(new EmbedCreateFields.Field() {
                @Override
                public String name() {
                    return "";
                }

                @Override
                public String value() {
                    return finalI + 1 + ".\t" + audioTrack.getInfo().title + "\t" + trackDuration;
                }

                @Override
                public boolean inline() {
                    return false;
                }
            });
        }
    }
}
