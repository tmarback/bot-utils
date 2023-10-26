package dev.sympho.bot_utils.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.checkerframework.dataflow.qual.SideEffectFree;
import org.immutables.value.Value;

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.event.AbstractRepliableContext;
import dev.sympho.bot_utils.event.ModalEventContext;
import dev.sympho.bot_utils.event.reply.InteractionReplyManager;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;

/**
 * Centralized manager for modal submission handling.
 *
 * @version 1.0
 * @since 1.0
 */
@Value.Enclosing
@Value.Style( 
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        overshadowImplementation = true
)
public class ModalManager extends ComponentManager<
                ModalSubmitInteractionEvent,
                ModalEventContext,
                ModalManager.HandlerFunction,
                ModalManager.Handler
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
    protected ModalEventContext makeContext( final ModalSubmitInteractionEvent event, 
            final AccessManager accessManager ) {

        return new ModalContextImpl( event, accessManager );

    }

    /**
     * A function used to handle a modal submit event.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface HandlerFunction extends ComponentManager.HandlerFunction<ModalEventContext> {}

    /**
     * Specification for the handling of a modal submission.
     *
     * @since 1.0
     */
    @Value.Immutable
    public interface Handler extends ComponentManager.Handler<HandlerFunction> {

        /**
         * Creates a handler that uses the given function..
         *
         * @param id The handler ID.
         * @param handler The function to handle events with.
         * @return The resulting handler.
         */
        static Handler of( final String id, final HandlerFunction handler ) {

            return builder()
                    .id( id )
                    .handler( handler )
                    .build();

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
        class Builder extends ImmutableModalManager.Handler.Builder {}

    }

    /**
     * The execution context of a modal being submitted.
     *
     * @since 1.0
     */
    private static final class ModalContextImpl 
            extends AbstractRepliableContext<ModalSubmitInteractionEvent>
            implements ModalEventContext {

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
                    false, false
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
