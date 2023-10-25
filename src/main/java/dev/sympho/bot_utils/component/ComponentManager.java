package dev.sympho.bot_utils.component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.sympho.bot_utils.access.AccessException;
import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.access.NamedGroup;
import dev.sympho.bot_utils.event.ComponentEventContext;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateFields.Field;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

/**
 * Centralized manager for component interaction handling.
 *
 * @param <E> The interaction event type.
 * @param <C> The context type.
 * @param <HF> The handler function type.
 * @param <H> The handler type.
 * @param <HE> The handler entry type.
 * @version 1.0
 * @since 1.0
 */
public abstract class ComponentManager<
                E extends @NonNull ComponentInteractionEvent, 
                C extends @NonNull ComponentEventContext,
                HF extends ComponentManager.@NonNull HandlerFunction<C>,
                H extends ComponentManager.@NonNull Handler<H, HF>,
                HE extends ComponentManager.@NonNull HandlerEntry<H>
        > {

    /** Logger. */
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    /** Client to receive interactions from. */
    private final GatewayDiscordClient client;
    /** Access manager. */
    private final AccessManager accessManager;
    /** Handlers to use for interactions. */
    private final Map<String, H> handlers;
    
    /** The currently active pipeline. */
    private @Nullable Disposable active;

    /**
     * Creates a new manager that receives interactions from the given client.
     *
     * @param client The client to receive interactions from.
     * @param accessManager The access manager to use.
     */
    @SideEffectFree
    public ComponentManager( final GatewayDiscordClient client, 
            final AccessManager accessManager ) {

        logger.debug( "Configuring manager" );

        this.client = client;
        this.accessManager = accessManager;
        this.handlers = new ConcurrentHashMap<>();

        this.active = null;

    }

    /**
     * Registers a handler.
     *
     * @param id The ID that invokes the handler.
     * @param handler The handler to use.
     */
    public void register( final String id, final H handler ) {

        logger.info( "Registering handler with ID {}", id );
        if ( handlers.put( id, handler ) != null ) {
            logger.warn( "Replaced handler with ID {}", id );
        }

    }

    /**
     * Registers an interaction handler.
     *
     * @param handler The handler to use.
     * @implSpec The default implementation directly delegates to 
     *           {@link #register(String, Handler)}. If the entry type has additional configuration
     *           options, this method should be overriden to use them.
     */
    public void register( final HE handler ) {

        register( handler.id(), handler.handler() );

    }

    /**
     * Unregisters an interaction handler, if it exists.
     *
     * @param id The ID that invokes the handler.
     */
    public void unregister( final String id ) {

        logger.info( "Unregistering handler with ID {}", id );
        if ( handlers.remove( id ) == null ) {
            logger.error( "Handler ID {} was not found", id );
        }

    }

    /**
     * Registers a group of handlers.
     *
     * @param handlers The handlers to register.
     * @apiNote This method is a convenience to register all handlers in a batch.
     */
    @SuppressWarnings( "HiddenField" )
    public void registerAll( final Collection<HE> handlers ) {

        handlers.forEach( this::register );

    }

    /**
     * Creates a custom ID for a component with arguments in a format that is compatible with
     * a manager.
     *
     * @param id The handler ID.
     * @param args The interaction arguments.
     * @return The assembled custom ID.
     */
    @SideEffectFree
    public static String makeId( final String id, final String args ) {

        return "%s:%s".formatted( id, args );

    }

    /**
     * Makes a link to the message that the component is on.
     *
     * @param event The event.
     * @return The link.
     */
    private static String makeMessageUrl( final ComponentInteractionEvent event ) {

        final var interaction = event.getInteraction();
        final var guild = interaction.getGuildId().map( Snowflake::asString ).orElse( "@me" );
        final var channel = interaction.getChannelId().asString();
        @SuppressWarnings( "optional:method.invocation" ) // Components always have the message
        final var message = interaction.getMessageId().orElseThrow().asString();
        return "https://discord.com/channels/%s/%s/%s".formatted( guild, channel, message );

    }

    /**
     * Formats a field that links to the message that the component is on.
     *
     * @param event The event.
     * @return The field.
     */
    protected static Field sourceField( final ComponentInteractionEvent event ) {

        return EmbedCreateFields.Field.of(
            "Source Message",
            makeMessageUrl( event ),
            false
        );
    }

    /**
     * Retrieves the interaction event type.
     *
     * @return The interaction event type.
     */
    @Pure
    protected abstract Class<E> getEventType();

    /**
     * Creates the interaction context.
     *
     * @param event The interaction event.
     * @param access The access manager.
     * @return The created context.
     */
    @SideEffectFree
    protected abstract C makeContext( E event, AccessManager access );

    /**
     * Validates that the interaction is allowed.
     *
     * @param context The interaction context.
     * @param handler The invoked handler.
     * @return A mono that completes empty if the interaction is allowed, otherwise
     *         issuing an error message to be sent to the user. Additionally, it may
     *         result in an {@link AccessException} error to indicate an access issue.
     */
    @SideEffectFree
    protected abstract Mono<String> validateInteraction( C context, H handler );

    /**
     * Reports an error back to the user.
     *
     * @param context The event context.
     * @param message The error message.
     * @return A Mono that completes after the error is reported.
     */
    @SideEffectFree
    @SuppressWarnings( "nullness:return" ) // Idk what it's on about
    private Mono<? extends @NonNull Object> reportFailure( final C context, final String message ) {

        return context.event()
                .reply( message )
                .withEphemeral( true ); // Don't spam errors for everyone

    }

    /**
     * Creates a handler function that just reports an error to the user.
     *
     * @param error The error to report.
     * @return The function.
     */
    @SideEffectFree
    private HandlerFunction<C> validationFailReporter( final String error ) {

        return ( ctx, args ) -> reportFailure( ctx, error );

    }

    /**
     * Handles an interaction.
     *
     * @param event The invoking event.
     * @return A Mono that completes when the interaction handling finishes.
     */
    private Mono<?> handle( final E event ) {

        final var customId = event.getCustomId();
        logger.debug( "Received event with ID {}", customId );

        final var call = customId.split( ":", 2 );
        final var id = call[0];
        final var args = call.length > 1 ? call[1] : "";

        final var handler = handlers.get( id );
        if ( handler == null ) {
            logger.error( "Received interaction with unknown ID {}", id );
            return Mono.empty();
        } else {
            logger.trace( "Found handler with ID {}", id );
        }

        final var context = makeContext( event, accessManager );

        return validateInteraction( context, handler )
                .map( this::validationFailReporter )
                .defaultIfEmpty( handler.handler() ) 
                .flatMap( h -> h.apply( context, args ) )
                .cast( Object.class )
                .onErrorResume( AccessException.class, ex -> {
                    final var message = ex.group instanceof NamedGroup g
                            ? String.format( 
                                "Only users in the %s group can do this.",
                                g.name()
                            ) : "You are not allowed to do this.";
                    return reportFailure( context, message );
                } )
                .doOnError( e -> logger.error( "Handler ID " + id + " threw an error", e ) )
                .onErrorComplete();

    }

    /**
     * Starts handling interactions.
     */
    @PostConstruct
    public synchronized void start() {

        stop();
        logger.info( "Starting handler" );
        this.active = client.on( getEventType() )
                .flatMap( this::handle )
                .subscribe();

    }

    /**
     * Stops handling interactions.
     */
    @PreDestroy
    public synchronized void stop() {

        if ( active != null ) {
            logger.info( "Stopping handler" );
            active.dispose();
            active = null;
        }

    }

    /**
     * A function used to handle an interaction event.
     *
     * @param <C> The context type.
     * @since 1.0
     */
    @FunctionalInterface
    protected interface HandlerFunction<C extends @NonNull ComponentEventContext> 
            extends BiFunction<C, String, Mono<?>> {

        /**
         * Handles an interaction event.
         *
         * @param context The interaction context.
         * @param args The argument string encoded in the components's custom ID. May be empty.
         * @return A Mono that completes once handling is completed. It may result in an 
         *         {@link AccessException} error to indicate that the user does not have access
         *         to perform this interaction.
         */
        @Override
        Mono<?> apply( C context, String args );

    }

    /**
     * Specification for the handling of an interaction.
     *
     * @param <SELF> The self type.
     * @param <HF> The handler function type.
     * @since 1.0
     * @apiNote Implementations may add more configuration values beyond the minimum
     *          required by this interface.
     */
    protected interface Handler<SELF, HF extends @NonNull HandlerFunction<?>> {

        /**
         * The handler function.
         *
         * @return The handler function.
         */
        HF handler();

        /**
         * Composes this handler by transforming its handler function.
         *
         * @param transform The function to apply on the handler.
         * @return The Handler obtained by transforming this handler's function.
         */
        SELF compose( UnaryOperator<HF> transform );

    }

    /**
     * Specification for a handler to be registered.
     *
     * @param <H> The handler type.
     * @since 1.0
     * @apiNote Implementations may add more configuration values beyond the minimum
     *          required by this interface.
     */
    protected interface HandlerEntry<H extends @NonNull Handler<H, ?>> {

        /**
         * The component ID.
         *
         * @return The component ID.
         */
        String id();

        /**
         * The handler to use.
         *
         * @return The handler to use.
         */
        H handler();

    }
    
}
