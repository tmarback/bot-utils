package dev.sympho.bot_utils.event.reply;

import java.util.function.Supplier;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.dataflow.qual.SideEffectFree;

import dev.sympho.bot_utils.event.reply.MessageReplyManager.MessageReplyBase;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;

/**
 * Reply manager for interaction-based commands.
 *
 * @version 1.0
 * @since 1.0
 */
public class InteractionReplyManager extends AbstractReplyManager {

    /** The title of the reference message. */
    private final String referenceTitle;

    /** Generator of the fields for the reference message. */
    private final Supplier<? extends Iterable<? extends Field>> referenceFields;

    /** The backing interaction event. */
    private final DeferrableInteractionEvent event;

    /**
     * Creates a new message.
     *
     * @param referenceTitle The title of the reference message.
     * @param referenceFields Generator of the fields for the reference message.
     * @param event The backing interaction event.
     * @param defaultPrivate Whether replies are private by default.
     */
    public InteractionReplyManager( 
            final String referenceTitle,
            final Supplier<? extends Iterable<? extends Field>> referenceFields,
            final DeferrableInteractionEvent event, 
            final boolean defaultPrivate 
    ) {

        super( defaultPrivate );

        this.referenceTitle = referenceTitle;
        this.referenceFields = referenceFields;

        this.event = event;

    }

    @Override
    protected Mono<Void> doDefer() {

        return event.deferReply();

    }

    @Override
    protected Mono<Reply> send( final @NonNegative int index, final ReplySpec spec ) {

        if ( !acked ) { // Initial response
            if ( index > 0 ) { // Sanity check index
                throw new IllegalStateException( "Sending non-intial response while never acked" );
            }

            return event.reply( spec.toInteractionReply( defaultPrivate ) ) // Initial response
                    .thenReturn( new InteractionReply( event ) );
        } else { // Followup
            return event.createFollowup( spec.toInteractionFollowup( defaultPrivate ) ) // Followup
                    .map( m -> new InteractionFollowup( 
                            index, 
                            m.getId(), m.getChannelId(), 
                            event ) 
                    );
        }

    }

    /**
     * Formats the reference message.
     *
     * @return The formatted message embed.
     */
    @SideEffectFree
    private EmbedCreateSpec formatReference() {

        return EmbedCreateSpec.builder()
                .color( Color.ORANGE )
                .title( referenceTitle )
                .addAllFields( referenceFields.get() )
                .timestamp( event.getInteraction().getId().getTimestamp() )
                .build();

    }

    /**
     * Creates the exception for an invalid reply type.
     *
     * @param reply The source reply.
     * @return The exception.
     */
    private IllegalStateException invalidReplyType( final Reply reply ) {

        return new IllegalStateException( 
                "Unexpected reply type " + reply.getClass().getName() 
        );

    }

    @Override
    protected ReplyManager doDetach() {

        final var convertedReplies = replies.stream()
                .map( r -> {
                    if ( r instanceof InteractionReply re ) {
                        return new InteractionReplyAdapter( re );
                    } else if ( r instanceof InteractionFollowup f ) {
                        return new MessageReplyManager.MessageReply( 
                                f.index(), 
                                f.id(), f.channel(), 
                                f.event().getClient()
                        );
                    } else {
                        throw invalidReplyType( r );
                    }
                } )
                .toList();

        final var interaction = event.getInteraction();

        final var last = convertedReplies.get( convertedReplies.size() - 1 );
        final Mono<Snowflake> lastId;
        if ( last instanceof InteractionReplyAdapter r ) {
            lastId = r.ids.map( i -> i.message() );
        } else if ( last instanceof MessageReplyManager.MessageReply r ) {
            lastId = Mono.just( r.id() );
        } else {
            throw invalidReplyType( last );
        }

        return new MessageReplyManager(
                convertedReplies, 
                this::formatReference, 
                lastId, 
                interaction.getChannel(), 
                interaction.getGuildId().isPresent()
                        ? interaction.getUser().getPrivateChannel()
                        : null, 
                defaultPrivate
        );

    }

    @Override
    public Mono<ReplyManager> detach() {

        return Mono.fromSupplier( this::doDetach )
                .transform( sendLock::guard );

    }

    /**
     * IDs that identify a message.
     *
     * @param message The message ID.
     * @param channel The channel ID.
     * @since 1.0
     */
    private record MessageIds(
            Snowflake message,
            Snowflake channel
    ) {}

    /**
     * A reply sent as an interaction reply.
     *
     * @since 1.0
     */
    private static class InteractionReply implements Reply {

        /** The backing event. */
        private final DeferrableInteractionEvent event;

        /** The message IDs (lazy-loaded). */
        private final Mono<MessageIds> ids;

        /**
         * Creates a new instance.
         *
         * @param event The backing event.
         */
        InteractionReply( final DeferrableInteractionEvent event ) {

            this.event = event;

            this.ids = event.getReply()
                    .map( m -> new MessageIds( m.getId(), m.getChannelId() ) )
                    .cache(); // IDs are static and can be cached after lazy-loading

        }

        @Override
        public @NonNegative int index() {

            return 0; // Original reply is always first

        }

        @Override
        public Mono<Message> message() {

            return event.getReply();

        }

        @Override
        public Mono<Snowflake> messageId() {

            return ids.map( MessageIds::message );

        }

        @Override
        public Mono<Message> edit( final ReplyEditSpec spec ) {

            return event.editReply( spec.toInteraction() );

        }

        @Override
        public Mono<Void> delete() {

            return event.deleteReply();

        }
        
    }

    /**
     * A reply sent as an interaction followup.
     *
     * @param index The reply index.
     * @param id The reply ID.
     * @param channel The reply channel.
     * @param event The backing event.
     * @since 1.0
     */
    private record InteractionFollowup(
            @NonNegative int index,
            Snowflake id,
            Snowflake channel,
            DeferrableInteractionEvent event
    ) implements Reply {

        @Override
        public Mono<Message> message() {

            return event.getClient().getMessageById( channel, id );

        }

        @Override
        public Mono<Snowflake> messageId() {

            return Mono.just( id );

        }

        @Override
        public Mono<Message> edit( final ReplyEditSpec spec ) {

            return event.editFollowup( id, spec.toInteraction() );

        }

        @Override
        public Mono<Void> delete() {

            return event.deleteFollowup( id );

        }
        
    }

    /**
     * Adapter to convert an interaction reply-based reply into a message-based
     * reply (mainly interfacing the need to fetch the reply to obtain the ID).
     *
     * @since 1.0
     */
    private static class InteractionReplyAdapter implements MessageReplyBase {

        /** The backing client. */
        private final GatewayDiscordClient client;

        /** The message IDs. */
        private final Mono<MessageIds> ids;

        /**
         * Creates a new instance.
         *
         * @param reply The original reply.
         */
        InteractionReplyAdapter( final InteractionReply reply ) {

            this.client = reply.event.getClient();

            this.ids = reply.ids;
            this.ids.subscribe(); // Fetch IDs eagerly before the interaction expires
            
        }

        @Override
        public @NonNegative int index() {

            return 0; // Original reply is always first

        }

        @Override
        public Mono<Message> message() {

            return ids.flatMap( i -> client.getMessageById( i.channel(), i.message() ) );

        }

        @Override
        public Mono<Snowflake> messageId() {

            return ids.map( MessageIds::message );

        }

    }
    
}
