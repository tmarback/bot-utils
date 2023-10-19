package dev.sympho.bot_utils.event;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

/**
 * The context of an event that is related to a message.
 *
 * @version 1.0
 * @since 1.0
 */
public interface MessageBasedContext extends ChannelEventContext {

    /**
     * Retrieves the message that the event originates from.
     *
     * @return The message, or an empty mono if the message cannot be retrieved.
     * @see #message()
     * @implSpec Unlike {@link #message()}, this method <i>may</i> fetch the message again
     *           rather than using the message included in the event, if there is one.
     */
    @SideEffectFree
    Mono<Message> fetchMessage();

    /**
     * Retrieves the message that the event originates from, as attached to the event.
     *
     * @return The message, or {@code null} if the message data is not attached to 
     *         the received event.
     * @see #fetchMessage()
     * @implSpec This method <i>always</i> returns the message that was included in
     *           the event.
     */
    @Pure
    @Nullable Message message();

    /**
     * Retrieves the ID of the message that the event originates from.
     *
     * @return The message ID.
     */
    @Pure
    Snowflake messageId();
    
}
