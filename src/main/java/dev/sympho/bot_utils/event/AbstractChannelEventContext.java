package dev.sympho.bot_utils.event;

import org.checkerframework.checker.nullness.qual.NonNull;

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.access.ChannelAccessValidator;
import discord4j.core.event.domain.Event;

/**
 * Convenience base for implementing a channel-bound event context.
 * 
 * <p>Most leaf interfaces here should be able to be implemented on top of this
 * class without any further overrides as they already have default methods pulling all
 * necessary data from the event.
 *
 * @param <E> The event type.
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractChannelEventContext<E extends @NonNull Event> 
        implements ChannelEventContext {

    /** The triggering event. */
    private final E event;
    /** The access validator. */
    private final ChannelAccessValidator validator;

    /**
     * Creates a new instance.
     *
     * @param event The triggering event.
     * @param accessManager The access manager to use.
     */
    @SuppressWarnings( { "nullness:argument", "this-escape" } ) // Initialized enough
    protected AbstractChannelEventContext( final E event, final AccessManager accessManager ) {

        this.event = event;
        this.validator = accessManager.validator( this );
        
    }

    @Override
    public E event() {
        return event;
    }

    @Override
    public ChannelAccessValidator validator() {
        return validator;
    }
    
}
