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

import dev.sympho.bot_utils.access.AccessManager;
import dev.sympho.bot_utils.access.AccessValidator;
import dev.sympho.bot_utils.access.ChannelAccessContext;
import dev.sympho.bot_utils.access.Group;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
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
                C extends ComponentManager.@NonNull ComponentContext<E>,
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
     *         issuing an error message to be sent to the user.
     */
    @SideEffectFree
    protected abstract Mono<String> validateInteraction( C context, H handler );

    /**
     * Creates a handler function that just reports an error to the user.
     *
     * @param error The error to report.
     * @return The function.
     */
    @SideEffectFree
    private HandlerFunction<C> validationFailReporter( final String error ) {

        return ( ctx, args ) -> ctx.getEvent()
                .reply( error )
                .withEphemeral( true ); // Don't spam errors for everyone

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
    protected interface HandlerFunction<C extends @NonNull ComponentContext<?>> 
            extends BiFunction<C, String, Mono<?>> {

        /**
         * Handles an interaction event.
         *
         * @param context The interaction context.
         * @param args The argument string encoded in the components's custom ID. May be empty.
         * @return A Mono that completes once handling is completed.
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

    /**
     * The execution context of a component being interacted with. 
     *
     * @param <E> The event type.
     * @since 1.0
     */
    public static class ComponentContext<E extends @NonNull ComponentInteractionEvent> 
            implements ChannelAccessContext, AccessValidator {
        
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
        protected ComponentContext( final E event, final AccessManager accessManager ) {

            this.event = event;
            this.validator = accessManager.validator( this );
            
        }

        /**
         * Retrieves the triggering event.
         *
         * @return The event.
         */
        public E getEvent() {
            return event;
        }

        @Override
        public GatewayDiscordClient getClient() {
            return event.getClient();
        }

        @Override
        public Mono<Guild> getGuild() {
            return event.getInteraction().getGuild();
        }

        @Override
        public @Nullable Snowflake getGuildId() {
            return event.getInteraction().getGuildId().orElse( null );
        }

        @Override
        public User getUser() {
            return event.getInteraction().getUser();
        }

        @Override
        public Mono<Member> getMember() {
            return Mono.justOrEmpty( event.getInteraction().getMember() );
        }

        @Override
        public Mono<Member> getMember( final Snowflake guildId ) {
            return event.getInteraction().getMember()
                    .map( m -> ( User ) m )
                    .orElse( getUser() )
                    .asMember( guildId );
        }

        @Override
        public Mono<MessageChannel> getChannel() {
            return event.getInteraction().getChannel();
        }

        @Override
        public Snowflake getChannelId() {
            return event.getInteraction().getChannelId();
        }

        /**
         * Retrieves the message that the component is attached to.
         * 
         * <p>If the message is ephemeral, only {@link #getMessageId() the ID}
         * will be present.
         *
         * @return The message, or {@code null} if the message is ephemeral.
         */
        @Pure
        public @Nullable Message getMessage() {
            return event.getMessage().orElse( null );
        }

        /**
         * Retrieves the ID of the message that the component is attached to.
         *
         * @return The message ID.
         */
        @Pure
        public Snowflake getMessageId() {
            return event.getMessageId();
        }

        /**
         * Retrieves the interaction associated with the event.
         *
         * @return The interaction.
         */
        @Pure
        public Interaction getInteraction() {
            return event.getInteraction();
        }

        @Override
        public Mono<Boolean> hasAccess( final Group group ) {
            return validator.hasAccess( group );
        }

    }
    
}
