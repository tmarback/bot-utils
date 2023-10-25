package dev.sympho.bot_utils.component;

import java.util.List;
import java.util.function.UnaryOperator;

import org.checkerframework.dataflow.qual.SideEffectFree;

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.access.Group;
import dev.sympho.bot_utils.access.Groups;
import dev.sympho.bot_utils.event.AbstractRepliableContext;
import dev.sympho.bot_utils.event.ButtonContext;
import dev.sympho.bot_utils.event.reply.InteractionReplyManager;
import dev.sympho.reactor_utils.concurrent.LockMap;
import dev.sympho.reactor_utils.concurrent.NonblockingLockMap;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import reactor.core.publisher.Mono;

/**
 * Centralized manager for button interaction handling.
 *
 * @version 1.0
 * @since 1.0
 */
public class ButtonManager extends ComponentManager<
                ButtonInteractionEvent,
                ButtonContext,
                ButtonManager.HandlerFunction,
                ButtonManager.Handler,
                ButtonManager.HandlerEntry
        > {

    /** Lock provider. */
    private final LockMap<Snowflake> locks;

    /**
     * Creates a new manager that receives interactions from the given client.
     *
     * @param client The client to receive interactions from.
     * @param accessManager The access manager to use.
     */
    @SideEffectFree
    public ButtonManager( final GatewayDiscordClient client, final AccessManager accessManager ) {

        super( client, accessManager );
        this.locks = new NonblockingLockMap<>();

    }

    /**
     * Creates a custom ID for a button with arguments in a format that is compatible with
     * this manager.
     *
     * @param id The button handler ID.
     * @param args The interaction arguments.
     * @return The assembled custom ID.
     */
    @SideEffectFree
    public static String makeId( final String id, final String args ) {

        return ComponentManager.makeId( id, args );

    }

    @Override
    public void register( final HandlerEntry handler ) {

        final var id = handler.id();
        final var handle = handler.handler();

        register( id, handler.mutex() ? handle.compose( this::mutex ) : handle );

    }

    @Override
    protected Class<ButtonInteractionEvent> getEventType() {

        return ButtonInteractionEvent.class;

    }

    @Override
    protected ButtonContext makeContext( final ButtonInteractionEvent event, 
            final AccessManager accessManager ) {

        return new ButtonContextImpl( event, accessManager );

    }

    @Override
    protected Mono<String> validateInteraction( final ButtonContext context, 
            final Handler handler ) {

        return context.validate( handler.group() ).cast( String.class );

    }

    /**
     * Converts a handler into a mutually-exclusive handler, where concurrent interactions 
     * to buttons in the same message (not necessarily the same button, but any button in
     * the message whose handler is also a mutex in this manager) will only allow one 
     * execution (namely the first) to proceeed while the others fail, until the executing
     * handler finishes.
     * 
     * <p>This method should be used to protect button-based features where only one execution
     * should be allowed at once. Preferrably it should be used in conjunction with disabling
     * the buttons, which by itself would not be sufficient due to the lag between the start
     * of handling and the edit being processed.
     *
     * @param handler The handler to wrap.
     * @return The wrapped mutex handler.
     */
    public HandlerFunction mutex( final HandlerFunction handler ) {

        return ( ctx, id ) -> {

            final var lock = locks.tryAcquire( ctx.messageId() );
            if ( lock == null ) {
                logger.debug( "Aborted due to lock acquire fail" );
                return ctx.event().reply()
                        .withEphemeral( true )
                        .withContent( "Sorry, please try again." );
            }

            return lock.releaseAfter( Mono.defer( () -> handler.apply( ctx, id ) ) );

        };

    }

    /**
     * A function used to handle a button press event.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface HandlerFunction extends ComponentManager.HandlerFunction<ButtonContext> {}

    /**
     * Specification for the handling of a button.
     *
     * @param handler The handler function.
     * @param group The group that the user must have access to in order to use the button.
     * @since 1.0
     */
    public record Handler( 
            HandlerFunction handler,
            Group group
    ) implements ComponentManager.Handler<Handler, HandlerFunction> {

        @Override
        public Handler compose( final UnaryOperator<HandlerFunction> transform ) {

            return new Handler( transform.apply( handler ), group );

        }

        /**
         * Creates a handler that uses the given function and requires the given group.
         *
         * @param handler The function to handle events with.
         * @param group The group that the user must have access to in order to use the button.
         * @return The resulting handler.
         */
        public static Handler of( final HandlerFunction handler, final Group group ) {

            return new Handler( handler, group );

        }

        /**
         * Creates a handler that uses the given function and requires no groups.
         *
         * @param handler The function to handle events with.
         * @return The resulting handler.
         */
        public static Handler of( final HandlerFunction handler ) {

            return of( handler, Groups.EVERYONE );

        }

    }

    /**
     * Specification for a handler to be registered.
     *
     * @param id The button ID.
     * @param handler The handler to use.
     * @param mutex If {@code true}, the given handler is converted into a 
     *              {@link ButtonManager#mutex(HandlerFunction) mutex} before registering.
     * @since 1.0
     */
    public record HandlerEntry(
            String id,
            Handler handler,
            boolean mutex
    ) implements ComponentManager.HandlerEntry<Handler> {

        /**
         * Creates a handler with the given ID that uses the given function and requires 
         * the given group.
         *
         * @param id The button ID.
         * @param handler The handler function to use.
         * @param mutex If {@code true}, the handler will be converted into a 
         *              {@link ButtonManager#mutex(HandlerFunction) mutex} before registering.
         * @param group The group that the user must have access to in order to use the button.
         * @return The resulting handler.
         */
        public static HandlerEntry of( final String id, 
                final HandlerFunction handler, 
                final boolean mutex, 
                final Group group ) {

            return new HandlerEntry( id, Handler.of( handler, group ), mutex );

        }

        /**
         * Creates a handler with the given ID that uses the given function and requires 
         * no groups.
         *
         * @param id The button ID.
         * @param handler The handler function to use.
         * @param mutex If {@code true}, the handler will be converted into a 
         *              {@link ButtonManager#mutex(HandlerFunction) mutex} before registering.
         * @return The resulting handler.
         */
        public static HandlerEntry of( final String id, 
                final HandlerFunction handler,
                final boolean mutex ) {

            return new HandlerEntry( id, Handler.of( handler ), mutex );

        }

    }

    /**
     * The execution context of a button being pressed.
     *
     * @since 1.0
     */
    private static final class ButtonContextImpl 
            extends AbstractRepliableContext<ButtonInteractionEvent> 
            implements ButtonContext {

        /**
         * Creates a new instance.
         *
         * @param event The triggering event.
         * @param accessManager The access manager to use.
         */
        ButtonContextImpl( 
                final ButtonInteractionEvent event, 
                final AccessManager accessManager 
        ) {

            super( event, accessManager, new InteractionReplyManager( 
                    "Button Pressed", 
                    () -> List.of(
                            ComponentManager.sourceField( event )
                    ), 
                    event, 
                    false
            ) );
            
        }

    }
    
}
