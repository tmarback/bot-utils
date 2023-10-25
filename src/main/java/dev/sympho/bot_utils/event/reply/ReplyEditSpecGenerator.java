package dev.sympho.bot_utils.event.reply;

import java.util.List;
import java.util.Optional;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.immutables.value.Value;

import dev.sympho.d4j_encoding_extra.MetaExtraEncodingEnabled;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.MetaEncodingEnabled;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;

/**
 * Specification for editing a previosly sent event reply.
 *
 * @version 1.0
 * @since 1.0
 */
@Value.Immutable
@SpecStyle
@MetaEncodingEnabled
@MetaExtraEncodingEnabled
@SuppressWarnings( { 
        "immutables:incompat", 
        "OverloadMethodsDeclarationOrder", 
        "optional.collection" 
} )
interface ReplyEditSpecGenerator {

    /**
     * The new reply message content.
     *
     * @return The value.
     */
    Possible<Optional<String>> content();

    /**
     * The new reply embeds.
     *
     * @return The value.
     */
    Possible<Optional<List<EmbedCreateSpec>>> embeds();

    /**
     * The new attached files.
     *
     * @return The value.
     */
    List<MessageCreateFields.File> files();

    /**
     * The new spoiler-tagged attached files.
     *
     * @return The value.
     */
    List<MessageCreateFields.FileSpoiler> fileSpoilers();

    /**
     * The new allowed mentions.
     *
     * @return The value.
     */
    Possible<Optional<AllowedMentions>> allowedMentions();

    /**
     * The new message components.
     *
     * @return The value.
     */
    Possible<Optional<List<LayoutComponent>>> components();

    /**
     * The new attachments.
     *
     * @return The value.
     */
    Possible<Optional<List<Attachment>>> attachments();

    /**
     * Converts this spec into a message edit spec.
     *
     * @return The converted spec.
     */
    @SideEffectFree
    default MessageEditSpec toMessage() {

        return MessageEditSpec.builder()
                .content( content() )
                .embeds( embeds() )
                .files( files() )
                .fileSpoilers( fileSpoilers() )
                .allowedMentions( allowedMentions() )
                .components( components() )
                .attachments( attachments() )
                .build();

    }

    /**
     * Converts a message edit spec into a command reply edit spec.
     *
     * @param spec The original spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplyEditSpec from( final MessageEditSpec spec ) {

        return ReplyEditSpec.builder()
                .content( spec.content() )
                .embeds( spec.embeds() )
                .files( spec.files() )
                .fileSpoilers( spec.fileSpoilers() )
                .allowedMentions( spec.allowedMentions() )
                .components( spec.components() )
                .attachments( spec.attachments() )
                .build();

    }

    /**
     * Converts this spec into an interaction reply edit spec.
     *
     * @return The converted spec.
     */
    @SideEffectFree
    default InteractionReplyEditSpec toInteraction() {

        return InteractionReplyEditSpec.builder()
                .content( content() )
                .embeds( embeds() )
                .files( files() )
                .fileSpoilers( fileSpoilers() )
                .allowedMentions( allowedMentions() )
                .components( components() )
                .attachments( attachments() )
                .build();

    }

    /**
     * Converts an interaction reply edit spec into a command reply edit spec.
     *
     * @param spec The original spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplyEditSpec from( final InteractionReplyEditSpec spec ) {

        return ReplyEditSpec.builder()
                .content( spec.content() )
                .embeds( spec.embeds() )
                .files( spec.files() )
                .fileSpoilers( spec.fileSpoilers() )
                .allowedMentions( spec.allowedMentions() )
                .components( spec.components() )
                .attachments( spec.attachments() )
                .build();

    }
    
}
