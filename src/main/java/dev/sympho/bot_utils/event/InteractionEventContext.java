package dev.sympho.bot_utils.event;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

/**
 * The context of an interaction-based event.
 *
 * @version 1.0
 * @since 1.0
 */
public interface InteractionEventContext extends ChannelEventContext {

    @Override
    InteractionCreateEvent event();

    /**
     * Retrieves the interaction associated with the event.
     *
     * @return The interaction.
     */
    @Pure
    default Interaction interaction() {
        return event().getInteraction();
    }

    @Override
    default Mono<Guild> guild() {
        return interaction().getGuild();
    }

    @Override
    default @Nullable Snowflake guildId() {
        return interaction().getGuildId().orElse( null );
    }

    @Override
    default User user() {
        return interaction().getUser();
    }

    @Override
    default Mono<Member> member() {
        return Mono.justOrEmpty( interaction().getMember() );
    }

    @Override
    default Mono<Member> member( final Snowflake guildId ) {
        return interaction().getMember()
                .map( m -> ( User ) m )
                .orElse( user() )
                .asMember( guildId );
    }

    @Override
    default Mono<MessageChannel> channel() {
        return interaction().getChannel();
    }

    @Override
    default Snowflake channelId() {
        return interaction().getChannelId();
    }
    
}
