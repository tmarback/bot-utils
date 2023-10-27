package dev.sympho.bot_utils.event.reply;

import java.util.Arrays;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

/**
 * Manages the replies sent by an event handler.
 * 
 * <p>Note that if a reply specification has the {@link ReplySpec#privately()} field as
 * absent, the default for the manager will be used. Furthermore, if the initial reply
 * was {@link #defer() deferred}, the value of that field on the first reply is ignored 
 * and the default is used instead (further replies are unaffected).
 * 
 * <p>Note also that not all managers are valid forever; for example, interaction-based
 * managers expire after 15 minutes (once the interaction token expires). If handling
 * takes a long time and requires sending further replies, use {@link #detach()} to
 * obtain an independent, long-term manager after sending the initial responses.
 *
 * @version 1.0
 * @since 1.0
 * @implSpec Implementations are required to be concurrency-safe.
 */
public interface ReplyManager {

    /** Error message used when trying to access the initial reply before sending one. */
    String NO_RESPONSE_ERROR = "No response made yet.";

    /**
     * Defers the initial response, signaling it to the user in some form. Whether it will
     * be done privately or not is determined by the manager's default.
     * 
     * <p>If a deferral was already made or a reply was already sent, this
     * method (and by extension the returned Mono) has no effect.
     *
     * @return A Mono that completes once deferral is complete.
     */
    Mono<Void> defer();

    /**
     * Sends a new reply.
     *
     * @param spec The reply specification.
     * @return The created reply.
     */
    Mono<Reply> add( ReplySpec spec );

    /**
     * Sends a new reply.
     *
     * @return A reply builder Mono that can be configured with the target reply then subscribed
     *         to send the reply.
     */
    default ReplyMono add() {

        return ReplyMono.of( this );

    }
    
    /**
     * Sends a new reply.
     *
     * @param spec The message specification.
     * @return The created reply.
     * @see ReplyUtils#from(MessageCreateSpec)
     * @see #add(CommandReplySpec)
     */
    default Mono<Reply> add( final MessageCreateSpec spec ) {

        return add( ReplySpecGenerator.from( spec ) );

    }

    /**
     * Sends a new reply.
     *
     * @param spec The message specification.
     * @return The created reply.
     * @see ReplyUtils#from(InteractionApplicationCommandCallbackSpec)
     * @see #add(CommandReplySpec)
     */
    default Mono<Reply> add( final InteractionApplicationCommandCallbackSpec spec ) {

        return add( ReplySpecGenerator.from( spec ) );

    }

    /**
     * Sends a new reply.
     *
     * @param spec The message specification.
     * @return The created reply.
     * @see ReplyUtils#from(InteractionFollowupCreateSpec)
     * @see #add(CommandReplySpec)
     */
    default Mono<Reply> add( final InteractionFollowupCreateSpec spec ) {

        return add( ReplySpecGenerator.from( spec ) );

    }

    /**
     * Sends a new reply.
     *
     * @param content The message content.
     * @return A reply builder Mono initialized with the given content.
     */
    default ReplyMono add( final String content ) {

        return add().withContent( content );

    }

    /**
     * Sends a new reply.
     *
     * @param embeds The message embeds.
     * @return A reply builder Mono initialized with the given embeds.
     */
    default ReplyMono add( final EmbedCreateSpec... embeds ) {

        return add().withEmbeds( Arrays.asList( embeds ) );

    }

    /**
     * Retrieves a reply.
     *
     * @param index The reply index.
     * @return The reply.
     * @throws IndexOutOfBoundsException if there is no reply with that index.
     */
    Reply get( @NonNegative int index ) throws IndexOutOfBoundsException;

    /**
     * Retrieves the initial reply.
     *
     * @return The reply.
     * @throws IllegalStateException if an initial reply was not sent yet.
     */
    default Reply get() throws IllegalStateException {

        try {
            return get( 0 );
        } catch ( final IndexOutOfBoundsException e ) {
            throw new IllegalStateException( NO_RESPONSE_ERROR );
        }

    }

    /**
     * Obtains a detached copy of this manager, where the existing reply chain is the same,
     * but new replies are sent independently.
     * 
     * <p>An initial reply <b>must</b> have already been sent before detaching (i.e.
     * {@link #get()} should not throw an exception); otherwise the returned mono will
     * finish with an error.
     * 
     * <p>The new manager may, in some cases, not use the same method to send replies (for 
     * example, it might use regular messages while the original manager uses interaction 
     * responses). This causes the side effect that, while existing replies are still 
     * accessible via {@link #get(int)}, some functionality may be unavailable (for example, 
     * ephemeral interaction responses).
     * 
     * <p>However, managers returned by this method are required to use a sending method that
     * does not expire, and so may be used in cases where the original reply manager might
     * expire before processing finishes.
     * 
     * <p>Note that some events (like slash commands) require a response to be sent natively
     * or else it is considered to have failed; a best-effort attempt is made to catch cases
     * where detaching is attempted before a required response and throw an exception early
     * for visibily, however not all cases can be caught as it might depend on actions outside
     * of the manager's control (for example a component interaction might be responded to by
     * editing the original message instead).
     *
     * @return A new detached manager. May result in an {@link IllegalStateException} if the
     *         manager detects that switching to a detached manager on the current state would
     *         cause an error or timeout later.
     * @apiNote The detachment process occurs at subscription time, and the source manager
     *          <b>must</b> be valid at that time, so make sure to do so <b>before</b> a
     *          long processing period if needed.
     */
    @SideEffectFree
    Mono<ReplyManager> detach();
    
}
