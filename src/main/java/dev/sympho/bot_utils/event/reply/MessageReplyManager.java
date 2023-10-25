package dev.sympho.bot_utils.event.reply;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.entity.channel.PrivateChannel;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

/**
 * Reply manager for message-based commands.
 *
 * @version 1.0
 * @since 1.0
 */
public class MessageReplyManager extends AbstractReplyManager {

    /** Where to send public replies. */
    private final ReplyChain publicChain;
    /** 
     * Where to send private replies. May be the same as {@link #publicChain} if the command
     * is called from a private channel. 
     */
    private final ReplyChain privateChain;

    /**
     * Creates a new manager.
     *
     * @param replies The existing replies.
     * @param publicChain The reply chain for public replies.
     * @param privateChain The reply chain for private replies. 
     *                     If {@code null}, the public chain is also used for private 
     *                     replies (so public and private replies are the same).
     * @param defaultPrivate Whether replies are private by default.
     */
    @SideEffectFree
    private MessageReplyManager( 
            final List<? extends Reply> replies,
            final ReplyChain publicChain,
            final @Nullable ReplyChain privateChain,
            final boolean defaultPrivate 
    ) {

        super( replies, defaultPrivate );

        this.publicChain = publicChain;
        this.privateChain = privateChain != null ? privateChain : publicChain;

    }

    /**
     * Creates a new manager.
     *
     * @param replies The existing replies.
     * @param referenceFormatter The formatter to use for the reference message.
     * @param previous The ID of the latest message in the public reply chain.
     * @param publicChannel The public channel to send messages in.
     * @param privateChannel The private channel to send messages in.
     *                       If {@code null}, the public channel is also used for private
     *                       replies (so public and private replies are the same).
     * @param defaultPrivate Whether replies are private by default.
     */
    @SideEffectFree
    MessageReplyManager( 
            final List<? extends Reply> replies,
            final Supplier<EmbedCreateSpec> referenceFormatter,
            final Mono<Snowflake> previous,
            final Mono<MessageChannel> publicChannel, 
            final @Nullable Mono<PrivateChannel> privateChannel,
            final boolean defaultPrivate 
    ) {

        this( 
                replies, 
                new ReplyChain( referenceFormatter, publicChannel, previous ),
                privateChannel != null
                        ? new ReplyChain( referenceFormatter, privateChannel, Mono.empty() )
                        : null,
                defaultPrivate
        );

    }

    /**
     * Creates a new manager.
     *
     * @param referenceFormatter The formatter to use for the reference message.
     * @param original The original message that triggered the event.
     * @param publicChannel The public channel to send messages in.
     * @param privateChannel The private channel to send messages in.
     *                       If {@code null}, the public channel is also used for private
     *                       replies (so public and private replies are the same).
     * @param defaultPrivate Whether replies are private by default.
     */
    @SideEffectFree
    public MessageReplyManager( 
            final Supplier<EmbedCreateSpec> referenceFormatter,
            final Message original, 
            final Mono<MessageChannel> publicChannel, 
            final @Nullable Mono<PrivateChannel> privateChannel,
            final boolean defaultPrivate
    ) {

        this(
                Collections.emptyList(),
                referenceFormatter,
                Mono.just( original.getId() ),
                publicChannel,
                privateChannel,
                defaultPrivate
        );

    }

    @Override
    public Mono<Void> doDefer() {

        return ( defaultPrivate ? privateChain : publicChain ).channel
                .flatMap( MessageChannel::type );

    }

    @Override
    protected Mono<Reply> send( final @NonNegative int index, final ReplySpec spec ) {

        final var channel = spec.privatelyOrElse( defaultPrivate ) ? privateChain : publicChain;
        return channel.send( index, spec );

    }

    @Override
    @SuppressWarnings( "interning:not.interned" ) // Intentional
    public ReplyManager doDetach() {

        return new MessageReplyManager(
                replies, 
                publicChain.copy(), 
                privateChain == publicChain ? null : privateChain.copy(), 
                defaultPrivate
        );

    }

    /**
     * The base for a message-based reply.
     *
     * @since 1.0
     */
    interface MessageReplyBase extends Reply {

        @Override
        default Mono<Message> edit( final ReplyEditSpec spec ) {

            return message().flatMap( m -> m.edit( spec.toMessage() ) );

        }

        @Override
        default Mono<Void> delete() {

            return message().flatMap( Message::delete );

        }

    }

    /**
     * A reply made by the manager.
     *
     * @param index The reply index.
     * @param id The ID of the reply message.
     * @param channel The ID of the channel the reply was sent in.
     * @param client The client used to connect to Discord.
     * @since 1.0
     */
    record MessageReply( 
            @NonNegative int index,
            Snowflake id,
            Snowflake channel,
            GatewayDiscordClient client
    ) implements MessageReplyBase {

        @Override
        public Mono<Message> message() {

            return client.getMessageById( channel, id );

        }

        @Override
        public Mono<Snowflake> messageId() {

            return Mono.just( id );

        }

    }

    /**
     * Manages a sequence of replies sent by the command.
     *
     * @since 1.0
     */
    private static class ReplyChain {

        /** The target channel. */
        public final Mono<? extends MessageChannel> channel;

        /** The formatter for the reference message. */
        private final Supplier<EmbedCreateSpec> referenceFormatter;
        
        /** The last sent reply ID, or {@code null} if none were sent in this channel yet. */
        private Mono<Snowflake> last;

        /**
         * Creates a new instance.
         *
         * @param referenceFormatter The formatter to use for the reference message.
         * @param channel The target channel.
         * @param last The last sent reply ID in this channel, or {@code null} 
         *             if none were sent in this channel yet.
         */
        ReplyChain( 
                final Supplier<EmbedCreateSpec> referenceFormatter,
                final Mono<? extends MessageChannel> channel, 
                final Mono<Snowflake> last 
        ) {

            this.referenceFormatter = Objects.requireNonNull( referenceFormatter );
            this.channel = Objects.requireNonNull( channel );
            this.last = last;

        }

        /**
         * Creates a reply chain that is initialized to a copy of the state of this chain
         * but is otherwise functions independently.
         *
         * @return The new reply chain.
         */
        public ReplyChain copy() {

            return new ReplyChain( referenceFormatter, channel, last );

        }

        /**
         * Sends the reference message to the given channel.
         *
         * @param ch The channel to send to.
         * @return The sent message ID.
         */
        private Mono<Snowflake> sendReference( final MessageChannel ch ) {

            return ch.createMessage( referenceFormatter.get() )
                    .map( Message::getId );

        }

        /**
         * Sends reply.
         *
         * @param index The reply index.
         * @param spec The reply specification.
         * @param channel The target channel.
         * @return The sent reply.
         */
        @SuppressWarnings( "HiddenField" )
        private Mono<Reply> send( final @NonNegative int index, final ReplySpec spec, 
                final MessageChannel channel ) {

            this.last = this.last.switchIfEmpty( Mono.defer( () -> sendReference( channel ) ) )
                    .map( spec.toMessage()::withMessageReference )
                    .flatMap( channel::createMessage )
                    .map( Message::getId )
                    .cache();

            return this.last.map( id -> new MessageReply( 
                    index, 
                    id, 
                    channel.getId(), 
                    channel.getClient() 
            ) );

        }

        /**
         * Sends reply.
         *
         * @param index The reply index.
         * @param spec The reply specification.
         * @return The sent reply.
         */
        public Mono<Reply> send( final @NonNegative int index, final ReplySpec spec ) {

            return channel.flatMap( ch -> send( index, spec, ch ) );

        }

    }
    
}
