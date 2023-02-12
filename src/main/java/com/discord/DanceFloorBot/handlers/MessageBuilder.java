package com.discord.DanceFloorBot.handlers;

import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Color;

import java.util.List;

public class MessageBuilder {
    public MessageBuilder() {}

    public EmbedCreateSpec getDefaultBuilder(String title, AudioTrack track, Color color) {
        return EmbedCreateSpec.builder()
                .color(color)
                .title(title)
                .addField("", track.getInfo().title, false)
                .url(track.getInfo().uri)
                .build();
    }

    public EmbedCreateSpec getPlayListBuilder(String title, String currentTrack, List<EmbedCreateFields.Field> fields, int page, int totalPages, boolean isShuffle) {
        String shuffle = "off";
        if (isShuffle) {
            shuffle = "on";
        }
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder()
                .color(Color.SEA_GREEN)
                .title(title)
                .addField(currentTrack, "", false)
                .addAllFields(fields)
                .footer("Page " + page + "/" + totalPages + " | Shuffle " + shuffle, null);
        return builder.build();
    }

    public EmbedCreateSpec getBlankBuilder(String title) {
        return EmbedCreateSpec.builder()
                .color(Color.SEA_GREEN)
                .title(title)
                .build();
    }

    public EmbedCreateSpec getBlankBuilderWithTrack(String title, AudioTrack track) {
        return EmbedCreateSpec.builder()
                .color(Color.SEA_GREEN)
                .title(title)
                .addField("", track.getInfo().title, false)
                .url(track.getInfo().uri)
               // .footer(track.getUserData(String.class), null)
                .build();
    }

    public EmbedCreateSpec getBlankBuilderWithPlayList(String title, AudioPlaylist playlist) {
        return EmbedCreateSpec.builder()
                .color(Color.SEA_GREEN)
                .title(title)
                .addField("", playlist.getName(), false)
                .build();
    }

    public MessageEditSpec editDefaultBuilder(String title, AudioTrack track, Color color) {
        return MessageEditSpec.builder()
                .addEmbed(getDefaultBuilder(title, track, color))
                .build();
    }

    public InteractionReplyEditSpec editPlayListBuilder(String title, String currentTrack, List<EmbedCreateFields.Field> fields, int page, int totalPages, boolean isShuffle) {
        return InteractionReplyEditSpec.builder()
                .addEmbed(getPlayListBuilder(title, currentTrack, fields, page, totalPages, isShuffle))
                .build();
    }
}
