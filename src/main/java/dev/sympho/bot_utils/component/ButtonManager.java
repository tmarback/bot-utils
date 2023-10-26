package dev.sympho.bot_utils.component;

import java.util.List;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.immutables.value.Value;

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.access.Group;
import dev.sympho.bot_utils.access.Groups;
import dev.sympho.bot_utils.event.AbstractRepliableContext;
import dev.sympho.bot_utils.event.ButtonEventContext;
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
@Value.Enclosing
@Value.Style( 
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true
)
public class ButtonManager extends ComponentManager<
                ButtonInteractionEvent,
                ButtonEventContext,
                ButtonManager.HandlerFunction,
                ButtonManager.Handler
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
    protected Class<ButtonInteractionEvent> getEventType() {

        return ButtonInteractionEvent.class;

    }

    @Override
    protected ButtonEventContext makeContext( final ButtonInteractionEvent event, 
            final AccessManager accessManager ) {

        return new ButtonContextImpl( event, accessManager );

    }

    @Override
    protected Mono<?> runHandler( 
            final Handler handler, 
            final ButtonEventContext context, 
            final String args 
    ) {

        return context.validate( handler.group() )
                .thenReturn( handler.mutex()
                        ? mutex( handler.handler() )
                        : handler.handler()
                )
                .flatMap( h -> h.apply( context, args ) );
        
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
    private HandlerFunction mutex( final HandlerFunction handler ) {

        return ( ctx, id ) -> {

            final var lock = locks.tryAcquire( ctx.messageId() );
            if ( lock == null ) {
                logger.debug( "Aborted due to lock acquire fail" );
                return reportFailure( ctx, "Sorry, please try again." );
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
    public interface HandlerFunction extends ComponentManager.HandlerFunction<ButtonEventContext> {}

    /**
     * Specification for the handling of a button.
     *
     * @since 1.0
     */
    @Value.Immutable
    public interface Handler extends ComponentManager.Handler<HandlerFunction> {

        /**
         * The group that the user must have access to in order to use the button.
         * 
         * <p>Defaults to everyone (no restriction).
         *
         * @return The group.
         */
        @Value.Default
        default Group group() {
            return Groups.EVERYONE;
        }

        /**
         * If {@code true}, then this handler will acquire a lock on the source message
         * before executing.
         * 
         * <p>In other words, for any given message, at any given time, there is <b>at most</b>
         * one handler currently executing that was triggered by a button on that message and
         * have this value as {@code true}. If a mutex event occurs on a message while there is
         * already one being handled, it will automatically fail and the user will be informed.
         * 
         * <p>Defaults to {@code false}.
         *
         * @return Whether the handler has mutually-exclusive execution.
         */
        @Value.Default
        default boolean mutex() {
            return false;
        }

        /**
         * Creates a new builder.
         *
         * @return The builder.
         */
        @SideEffectFree
        static Builder builder() {
            return new Builder();
        }

        /**
         * Creates a new builder initialized with the properties of the given handler.
         *
         * @param base The base instance to copy.
         * @return The builder.
         */
        @SideEffectFree
        static Builder builder( final Handler base ) {
            return builder().from( base );
        }
        
        /**
         * The default builder.
         *
         * @since 1.0
         */
        @SuppressWarnings( "MissingCtor" )
        class Builder extends ImmutableButtonManager.Handler.Builder {}

    }

    /**
     * The execution context of a button being pressed.
     *
     * @since 1.0
     */
    private static final class ButtonContextImpl 
            extends AbstractRepliableContext<ButtonInteractionEvent> 
            implements ButtonEventContext {

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
                    false, false
            ) );
            
        }

    }
    
}
