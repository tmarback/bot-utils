package dev.sympho.bot_utils.event;

import org.checkerframework.checker.nullness.qual.NonNull;

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.access.AccessValidator;
import discord4j.core.event.domain.Event;

/**
 * Convenience base for implementing an event context.
 * 
 * <p>Most leaf interfaces here should be able to be implemented on top of this
 * class without any further overrides as they already have default methods pulling all
 * necessary data from the event.
 *
 * @version 1.0
 * @since 1.0
 */
public abstract class AbstractEventContext<E extends @NonNull Event> implements EventContext {

    /** The triggering event. */
    private final E event;
    /** The access validator. */
    private final AccessValidator validator;

    /**
     * Creates a new instance.
     *
     * @param event The triggering event.
     * @param accessManager The access manager to use.
     */
    @SuppressWarnings( "nullness:argument" ) // Initialized enough
    protected AbstractEventContext( final E event, final AccessManager accessManager ) {

        this.event = event;
        this.validator = accessManager.validator( this );
        
    }

    @Override
    public E event() {
        return event;
    }

    @Override
    public AccessValidator validator() {
        return validator;
    }
    
}
