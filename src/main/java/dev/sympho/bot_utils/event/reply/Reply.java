package dev.sympho.bot_utils.event.reply;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageEditSpec;
import reactor.core.publisher.Mono;

/**
 * A reply made by an event handler.
 *
 * @version 1.0
 * @since 1.0
 */
public interface Reply {

    /**
     * The index of the reply in the sequence of replies sent by the handler.
     *
     * @return The index of the reply.
     * @see ReplyManager#get(int)
     */
    @Pure
    @NonNegative int index();

    /**
     * Retrieves the reply message.
     *
     * @return The reply message.
     */
    @SideEffectFree
    Mono<Message> message();

    /**
     * Retrieves the reply message's ID.
     *
     * @return The reply message ID.
     */
    @SideEffectFree
    Mono<Snowflake> messageId();

    /**
     * Edits the reply message.
     *
     * @param spec The edit specification.
     * @return The edited message.
     */
    Mono<Message> edit( ReplyEditSpec spec );

    /**
     * Edits the reply message.
     *
     * @return An edit builder Mono that can be configured with the target edit then subscribed
     *         to apply the edit.
     */
    default ReplyEditMono edit() {

        return ReplyEditMono.of( this );

    }

    /**
     * Edits the reply message.
     *
     * @param spec The edit specification.
     * @return The edited message.
     * @see ReplyUtils#from(MessageEditSpec)
     * @see #edit(ReplyEditSpec)
     */
    default Mono<Message> edit( final MessageEditSpec spec ) {

        return edit( ReplyUtils.from( spec ) );

    }

    /**
     * Edits the reply message.
     *
     * @param spec The edit specification.
     * @return The edited message.
     * @see ReplyUtils#from(InteractionReplyEditSpec)
     * @see #edit(ReplyEditSpec)
     */
    default Mono<Message> edit( final InteractionReplyEditSpec spec ) {

        return edit( ReplyUtils.from( spec ) );

    }

    /**
     * Deletes the reply message.
     *
     * @return A mono that completes when the message has been deleted.
     */
    Mono<Void> delete();
    
}
