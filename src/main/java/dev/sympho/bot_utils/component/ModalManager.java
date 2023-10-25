package dev.sympho.bot_utils.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.checkerframework.dataflow.qual.SideEffectFree;

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.event.AbstractRepliableContext;
import dev.sympho.bot_utils.event.ModalContext;
import dev.sympho.bot_utils.event.reply.InteractionReplyManager;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;
import reactor.core.publisher.Mono;

/**
 * Centralized manager for modal submission handling.
 *
 * @version 1.0
 * @since 1.0
 */
public class ModalManager extends ComponentManager<
                ModalSubmitInteractionEvent,
                ModalContext,
                ModalManager.HandlerFunction,
                ModalManager.Handler,
                ModalManager.HandlerEntry
        > {

    /**
     * Creates a new manager that receives interactions from the given client.
     *
     * @param client The client to receive interactions from.
     * @param accessManager The access manager to use.
     */
    @SideEffectFree
    public ModalManager( final GatewayDiscordClient client, final AccessManager accessManager ) {

        super( client, accessManager );

    }

    /**
     * Creates a custom ID for a modal with arguments in a format that is compatible with
     * this manager.
     *
     * @param id The modal handler ID.
     * @param args The interaction arguments.
     * @return The assembled custom ID.
     */
    @SideEffectFree
    public static String makeId( final String id, final String args ) {

        return ComponentManager.makeId( id, args );

    }

    @Override
    protected Class<ModalSubmitInteractionEvent> getEventType() {

        return ModalSubmitInteractionEvent.class;

    }

    @Override
    protected ModalContext makeContext( final ModalSubmitInteractionEvent event, 
            final AccessManager accessManager ) {

        return new ModalContextImpl( event, accessManager );

    }

    @Override
    protected Mono<String> validateInteraction( final ModalContext context, 
            final Handler handler ) {

        return Mono.empty();

    }

    /**
     * A function used to handle a modal submit event.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface HandlerFunction extends ComponentManager.HandlerFunction<ModalContext> {}

    /**
     * Specification for the handling of a modal submission.
     *
     * @param handler The handler function.
     * @since 1.0
     */
    public record Handler( 
            HandlerFunction handler
    ) implements ComponentManager.Handler<Handler, HandlerFunction> {

        @Override
        public Handler compose( final UnaryOperator<HandlerFunction> transform ) {

            return new Handler( transform.apply( handler ) );

        }

        /**
         * Creates a handler that uses the given function..
         *
         * @param handler The function to handle events with.
         * @return The resulting handler.
         */
        public static Handler of( final HandlerFunction handler ) {

            return new Handler( handler );

        }

    }

    /**
     * Specification for a handler to be registered.
     *
     * @param id The modal ID.
     * @param handler The handler to use.
     * @since 1.0
     */
    public record HandlerEntry(
            String id,
            Handler handler
    ) implements ComponentManager.HandlerEntry<Handler> {

        /**
         * Creates a handler with the given ID that uses the given function.
         *
         * @param id The modal ID.
         * @param handler The handler function to use.
         * @return The resulting handler.
         */
        public static HandlerEntry of( final String id, 
                final HandlerFunction handler ) {

            return new HandlerEntry( id, Handler.of( handler ) );

        }

    }

    /**
     * The execution context of a modal being submitted.
     *
     * @since 1.0
     */
    private static final class ModalContextImpl 
            extends AbstractRepliableContext<ModalSubmitInteractionEvent>
            implements ModalContext {

        /** The input fields in the modal, keyed by custom ID. */
        private final Map<String, TextInput> fields;

        /**
         * Creates a new instance.
         *
         * @param event The triggering event.
         * @param accessManager The access manager to use.
         */
        ModalContextImpl( final ModalSubmitInteractionEvent event, 
                final AccessManager accessManager ) {

            super( event, accessManager, new InteractionReplyManager( 
                    "Form Submitted", 
                    () -> List.of(
                            ComponentManager.sourceField( event )
                    ), 
                    event, 
                    false
            ) );

            this.fields = event.getComponents( TextInput.class )
                    .stream()
                    .collect( Collectors.toUnmodifiableMap( 
                            TextInput::getCustomId, 
                            Function.identity() 
                    ) );
            
        }

        @Override
        public TextInput getField( final String fieldId ) throws IllegalArgumentException {

            final var val = fields.get( fieldId );
            if ( val == null ) {
                throw new IllegalArgumentException( "Input field " + fieldId + " not present" );
            }
            return val;

        }

        @Override
        public Collection<TextInput> getFields() {

            return fields.values(); // Already pre-filtered for type.

        }

    }
    
}
