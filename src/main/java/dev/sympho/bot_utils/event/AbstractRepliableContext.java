package dev.sympho.bot_utils.event;

import org.checkerframework.checker.nullness.qual.NonNull;

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.event.reply.ReplyManager;
import discord4j.core.event.domain.Event;

/**
 * Convenience base for implementing an event context with reply functionality.
 *
 * @param <E> The event type.
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractRepliableContext<E extends @NonNull Event> 
        extends AbstractChannelEventContext<E>
        implements RepliableContext {

    /** The reply manager. */
    private final ReplyManager replyManager;

    /**
     * Creates a new instance.
     *
     * @param event The triggering event.
     * @param accessManager The access manager to use.
     * @param replyManager The reply manager to use.
     */
    protected AbstractRepliableContext( 
            final E event, 
            final AccessManager accessManager,
            final ReplyManager replyManager
    ) {

        super( event, accessManager );

        this.replyManager = replyManager;

    }

    @Override
    public ReplyManager replies() {

        return replyManager;

    }
    
}
