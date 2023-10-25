package dev.sympho.bot_utils.event.reply;

import java.util.Collections;
import java.util.List;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.immutables.value.Value;

import dev.sympho.d4j_encoding_extra.MetaExtraEncodingEnabled;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.discordjson.MetaEncodingEnabled;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;

/**
 * Specification for creating a new reply to an event.
 *
 * @version 1.0
 * @since 1.0
 */
@Value.Immutable
@SpecStyle
@MetaEncodingEnabled
@MetaExtraEncodingEnabled
@SuppressWarnings( { "immutables:incompat", "OverloadMethodsDeclarationOrder" } )
interface ReplySpecGenerator {

    /**
     * Whether to send the reply privately. If absent, the default for the command should be used.
     *
     * @return The value.
     */
    Possible<Boolean> privately();

    /**
     * The reply message content.
     *
     * @return The value.
     */
    Possible<String> content();

    /**
     * Whether the reply should use TTS.
     *
     * @return The value.
     */
    Possible<Boolean> tts();

    /**
     * The reply embeds.
     *
     * @return The value.
     */
    Possible<List<EmbedCreateSpec>> embeds();

    /**
     * The attached files.
     *
     * @return The value.
     */
    List<MessageCreateFields.File> files();

    /**
     * The spoiler-tagged attached files.
     *
     * @return The value.
     */
    List<MessageCreateFields.FileSpoiler> fileSpoilers();

    /**
     * The allowed mentions.
     *
     * @return The value.
     */
    Possible<AllowedMentions> allowedMentions();

    /**
     * The message components.
     *
     * @return The value.
     */
    Possible<List<LayoutComponent>> components();

    /**
     * Converts this spec into a message spec.
     *
     * @return The converted spec.
     */
    @SideEffectFree
    default MessageCreateSpec toMessage() {

        return MessageCreateSpec.builder()
                .content( content() )
                .tts( tts() )
                .embeds( embeds() )
                .files( files() )
                .fileSpoilers( fileSpoilers() )
                .allowedMentions( allowedMentions() )
                .components( components() )
                .build();

    }

    /**
     * Converts a message spec into a command reply spec.
     *
     * @param spec The original spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplySpec from( final MessageCreateSpec spec ) {

        return ReplySpec.builder()
                .content( spec.content() )
                .tts( spec.tts() )
                .embeds( spec.embeds() )
                .files( spec.files() )
                .fileSpoilers( spec.fileSpoilers() )
                .allowedMentions( spec.allowedMentions() )
                .components( spec.components() )
                .build();

    }

    /**
     * Converts this spec into an interaction reply spec.
     *
     * @param defaultPrivate Whether the reply should be private (ephemeral) by default
     *                       (if not specified).
     * @return The converted spec.
     */
    @SideEffectFree
    default InteractionApplicationCommandCallbackSpec toInteractionReply( 
            final boolean defaultPrivate ) {

        return InteractionApplicationCommandCallbackSpec.builder()
                .content( content() )
                .tts( tts() )
                .files( files() )
                .fileSpoilers( fileSpoilers() )
                .embeds( embeds() )
                .allowedMentions( allowedMentions() )
                .components( components() )
                .ephemeral( privately().toOptional().orElse( defaultPrivate ) )
                .build();

    }

    /**
     * Converts an interaction reply spec into a command reply spec.
     *
     * @param spec The original spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplySpec from( final InteractionApplicationCommandCallbackSpec spec ) {

        return ReplySpec.builder()
                .content( spec.content() )
                .tts( spec.tts() )
                .files( spec.files() )
                .fileSpoilers( spec.fileSpoilers() )
                .embeds( spec.embeds() )
                .allowedMentions( spec.allowedMentions() )
                .components( spec.components() )
                .privately( spec.ephemeral() )
                .build();

    }

    /**
     * Converts this spec into an interaction followup spec.
     *
     * @param defaultPrivate Whether the reply should be private (ephemeral) by default
     *                       (if not specified).
     * @return The converted spec.
     */
    @SideEffectFree
    default InteractionFollowupCreateSpec toInteractionFollowup( final boolean defaultPrivate ) {

        return InteractionFollowupCreateSpec.builder()
                .content( content() )
                .tts( tts().toOptional().orElse( false ) )
                .files( files() )
                .fileSpoilers( fileSpoilers() )
                .embeds( embeds().toOptional().orElse( Collections.emptyList() ) )
                .allowedMentions( allowedMentions() )
                .components( components() )
                .ephemeral( privately().toOptional().orElse( defaultPrivate ) )
                .build();

    }

    /**
     * Converts an interaction followup spec into a command reply spec.
     *
     * @param spec The original spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplySpec from( final InteractionFollowupCreateSpec spec ) {

        return ReplySpec.builder()
                .content( spec.content() )
                .tts( spec.tts() )
                .files( spec.files() )
                .fileSpoilers( spec.fileSpoilers() )
                .embeds( spec.embeds() )
                .allowedMentions( spec.allowedMentions() )
                .components( spec.components() )
                .privately( spec.ephemeral() )
                .build();

    }
    
}
