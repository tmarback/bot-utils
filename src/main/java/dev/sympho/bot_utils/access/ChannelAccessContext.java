package dev.sympho.bot_utils.access;

import java.util.Objects;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Mono;

/**
 * Access context that is specific to a channel.
 *
 * @version 1.0
 * @since 1.0
 * @implSpec A context must be effectively constant; that is, an implementation of this interface
 *           must always return the same value on methods that return a direct value (such as
 *           {@link #user()} or {@link #guildId()}). Methods that fetch remote resources
 *           (i.e. that return a Mono) may return different objects over time (as the remote object
 *           is modified), but must always reference the same entity.
 */
public interface ChannelAccessContext extends AccessContext {

    /**
     * Retrieves the channel.
     *
     * @return The channel.
     */
    @SideEffectFree
    Mono<? extends Channel> channel();

    /**
     * Retrieves the ID of the channel.
     *
     * @return The channel's ID.
     */
    @Pure
    Snowflake channelId();

    /**
     * Creates a copy of this context with the user replaced by the given user.
     * 
     * <p>All associated values are also replaced accordingly.
     *
     * @param user The target user.
     * @return The new context.
     */
    @Override
    default ChannelAccessContext asUser( final User user ) {

        if ( Objects.equals( user.getId(), user().getId() ) ) {
            return this;
        }

        return new UserOverrideChannelAccessContext( this, user );

    }
    
}
