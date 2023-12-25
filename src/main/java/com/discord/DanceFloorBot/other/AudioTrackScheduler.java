package com.discord.DanceFloorBot.other;

import com.discord.DanceFloorBot.handlers.CommandMethodsHandler;
import com.discord.DanceFloorBot.handlers.MessageBuilder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.rest.util.Color;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
public final class AudioTrackScheduler extends AudioEventAdapter {
    private final CommandMethodsHandler commandMethodsHandler = new CommandMethodsHandler();
    private final MessageBuilder messageBuilder = new MessageBuilder();
    private final List<AudioTrack> playedTracks;
    private final List<AudioTrack> queue;
    private final AudioPlayer player;
    private ChatInputInteractionEvent event;
    private boolean isLooped = false;
    private AudioTrack audioTrack;
    private boolean priority = false;
    private Snowflake messageId;
    private boolean isShuffle = false;
    private boolean showingPlayedTracks = false;


    public AudioTrackScheduler(final AudioPlayer player) {
        playedTracks = Collections.synchronizedList(new LinkedList<>());
        queue = Collections.synchronizedList(new LinkedList<>());
        this.player = player;
    }

    public Optional<AudioTrack> getCurrentTrack() {
        return Optional.ofNullable(player.getPlayingTrack());
    }

    public boolean playPrevious() {
        if (queue.isEmpty() || player.getPlayingTrack() == null) {
            return false;
        }
        queue.add(0, player.getPlayingTrack());
        player.stopTrack();
        return playNext();
    }

    public void repeat() {
        AudioTrack currentTrack = audioTrack;
        if (currentTrack != null) {
            player.playTrack(currentTrack.makeClone());
        }
    }

    public boolean playNext() {
        return !queue.isEmpty() && play(queue.remove(0), true);
    }

    public boolean skip() {
        return !queue.isEmpty() && play(queue.remove(0), true);
    }

    public void skipFirst(int number) {
        if (number > 0) {
            if (number >= queue.size()) {
                queue.clear();
            } else {
                queue.subList(0, number).clear();
            }
        }
    }

    public void skipFirst() {
        queue.remove(0);
    }

    public void skipRange(int start, int end) {
        if (end >= queue.size()) {
            queue.clear();
        } else {
            queue.subList(start - 1, end - 1).clear();
        }

    }

    public void pause() {
        player.setPaused(true);
    }

    public void restart() {
        player.setPaused(false);
    }

    public void stop() {
        player.stopTrack();
    }

    public Optional<List<AudioTrack>> getQueue() {
        return Optional.ofNullable(queue);
    }

    public boolean play(final AudioTrack track) {
        return play(track, false);
    }

    public boolean play(final AudioTrack track, final boolean force) {
        final boolean playing = player.startTrack(track, !force);
        if (!playing) {
            if (!priority) {
                queue.add(track);
            } else {
                queue.add(0, track);
            }
        }
        return playing;
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if (isShuffle()) {
            Collections.shuffle(queue);
        }
        this.audioTrack = track;
        Snowflake channelId = event.getInteraction().getChannelId();
        if (!isLooped()) {
            event.getClient().getChannelById(channelId)
                    .ofType(GuildMessageChannel.class)
                    .flatMap(channel -> channel.createMessage(
                            messageBuilder.getDefaultBuilder("Now playing!", track, Color.SEA_GREEN)))
                    .map(Message::getId)
                    .subscribe(messageId -> this.messageId = messageId);
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        Snowflake channelId = event.getInteraction().getChannelId();
        event.getClient().getChannelById(channelId)
                .ofType(GuildMessageChannel.class)
                .flatMap(channel -> channel.getMessageById(messageId)
                        .flatMap(msg -> {
                            if (msg == null) {
                                return Mono.empty();
                            }
                            return msg.edit(messageBuilder.editDefaultBuilder(
                                    "Can't play current track", track, Color.RED));
                        }))
                .subscribe();
    }

    @Override
    public void onTrackEnd(final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            if (isLooped()) {
                repeat();
            } else {
                playNext();
                playedTracks.add(track);
            }
        }
        else {
            playedTracks.add(track);
        }
    }
}
