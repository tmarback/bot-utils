package dev.sympho.bot_utils.event;

import org.checkerframework.dataflow.qual.Pure;

import dev.sympho.bot_utils.event.reply.Reply;
import dev.sympho.bot_utils.event.reply.ReplyManager;
import dev.sympho.bot_utils.event.reply.ReplyMono;
import dev.sympho.bot_utils.event.reply.ReplySpec;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

/**
 * Context for an event that can be replied to.
 *
 * @version 1.0
 * @since 1.0
 */
public interface RepliableContext extends ChannelEventContext {

    /**
     * Retrieves the initial reply manager for this instance.
     *
     * @return The reply manager.
     * @apiNote Note that the manager <i>may</i> expire after a certain time on some types
     *          of events; use {@link ReplyManager#detach()} to obtain a long-term manager
     *          before doing processing that may take a long time.
     *          This is not necessary if the specific event type guarantees a non-expiring
     *          manager.
     */
    @Pure
    ReplyManager replies();

    /**
     * Sends a reply, as if by calling 
     * {@link #replies()}.{@link ReplyManager#add() add()}.
     * 
     * <p>Sending more than one causes the replies to be chained
     * (each replying to the previous one).
     *
     * @return A reply builder Mono that can be configured with the target reply then subscribed
     *         to send the reply.
     * @see #replies()
     * @see ReplyManager#add()
     */
    default ReplyMono reply() {

        return replies().add();

    }

    /**
     * Sends a reply, as if by calling 
     * {@link #replies()}.{@link ReplyManager#add(String) add()}.
     * 
     * <p>Sending more than one causes the replies to be chained
     * (each replying to the previous one).
     *
     * @param content The message content.
     * @return A reply builder Mono initialized with the given content.
     * @see #replies()
     * @see ReplyManager#add(String)
     */
    default ReplyMono reply( final String content ) {

        return replies().add( content );

    }

    /**
     * Sends a reply, as if by calling 
     * {@link #replies()}.{@link ReplyManager#add(EmbedCreateSpec...) add()}.
     * 
     * <p>Sending more than one causes the replies to be chained
     * (each replying to the previous one).
     *
     * @param embeds The message embeds.
     * @return A reply builder Mono initialized with the given embeds.
     * @see #replies()
     * @see ReplyManager#add(EmbedCreateSpec...)
     */
    default ReplyMono reply( final EmbedCreateSpec... embeds ) {

        return replies().add( embeds );

    }

    /**
     * Sends a reply, as if by calling 
     * {@link #replies()}.{@link ReplyManager#add(MessageCreateSpec) add()}.
     * 
     * <p>Sending more than one causes the replies to be chained
     * (each replying to the previous one).
     *
     * @param spec The message specification.
     * @return The message.
     * @see #replies()
     * @see ReplyManager#add(MessageCreateSpec)
     */
    default Mono<Reply> reply( final MessageCreateSpec spec ) {

        return replies().add( spec );

    }

    /**
     * Sends a reply, as if by calling 
     * {@link #replies()}.{@link ReplyManager#add(InteractionApplicationCommandCallbackSpec) add()}.
     * 
     * <p>Sending more than one causes the replies to be chained
     * (each replying to the previous one).
     *
     * @param spec The message specification.
     * @return The message.
     * @see #replies()
     * @see ReplyManager#add(InteractionApplicationCommandCallbackSpec)
     */
    default Mono<Reply> reply( final InteractionApplicationCommandCallbackSpec spec ) {

        return replies().add( spec );

    }

    /**
     * Sends a reply, as if by calling 
     * {@link #replies()}.{@link ReplyManager#add(InteractionFollowupCreateSpec) add()}.
     * 
     * <p>Sending more than one causes the replies to be chained
     * (each replying to the previous one).
     *
     * @param spec The message specification.
     * @return The message.
     * @see #replies()
     * @see ReplyManager#add(InteractionFollowupCreateSpec)
     */
    default Mono<Reply> reply( final InteractionFollowupCreateSpec spec ) {

        return replies().add( spec );

    }

    /**
     * Sends a reply, as if by calling 
     * {@link #replies()}.{@link ReplyManager#add(CommandReplySpec) add()}.
     * 
     * <p>Sending more than one causes the replies to be chained
     * (each replying to the previous one).
     *
     * @param spec The message specification.
     * @return The message.
     * @see #replies()
     * @see ReplyManager#add(ReplySpec)
     */
    default Mono<Reply> reply( final ReplySpec spec ) {

        return replies().add( spec );

    }
    
}
