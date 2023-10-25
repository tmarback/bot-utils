package dev.sympho.bot_utils.event.reply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.dataflow.qual.SideEffectFree;

import dev.sympho.reactor_utils.concurrent.AsyncLock;
import dev.sympho.reactor_utils.concurrent.ReactiveLock;
import reactor.core.publisher.Mono;

/**
 * Base implementation for a reply manager.
 *
 * @version 1.0
 * @since 1.0
 */
abstract class AbstractReplyManager implements ReplyManager {

    /** The sent replies. */
    protected final List<Reply> replies;
    /** The lock for send ordering. */
    protected final ReactiveLock sendLock;

    /** Whether replies are private by default. */
    protected final boolean defaultPrivate;

    /** Whether the event has been acknowledged (first reply deferred or sent). */
    protected boolean acked;

    /**
     * Creates a new instance.
     *
     * @param replies The replies to initialize with.
     * @param defaultPrivate Whether replies are private by default.
     */
    AbstractReplyManager( final List<? extends Reply> replies, final boolean defaultPrivate ) {

        this.defaultPrivate = defaultPrivate;
        this.acked = false;

        this.replies = new ArrayList<>( replies );
        this.sendLock = new AsyncLock();

    }

    /**
     * Creates a new instance.
     *
     * @param defaultPrivate Whether replies are private by default.
     */
    AbstractReplyManager( final boolean defaultPrivate ) {

        this( Collections.emptyList(), defaultPrivate );

    }

    /**
     * Applies deferral to the initial response.
     *
     * @return A Mono that completes once deferral is complete.
     * @implSpec This method does not need to manage concurrency. This class ensures that
     *           each call to it only occurs after the previous reply is complete.
     *           It also ensures that it is only called if there has not been any prior
     *           deferral and no replies were sent yet.
     */
    protected abstract Mono<Void> doDefer();

    @Override
    public Mono<Void> defer() {

        return Mono.defer( () -> this.acked
                ? Mono.empty()
                : this.doDefer().doOnSuccess( s -> {
                    this.acked = true;
                } )
        ).transform( sendLock::guard );

    }

    /**
     * Sends a new reply.
     *
     * @param index The index of the created reply.
     * @param spec The reply specification.
     * @return The created reply.
     * @implSpec This method does not need to manage concurrency. This class ensures that
     *           each call to it only occurs after the previous reply is complete.
     */
    protected abstract Mono<Reply> send( @NonNegative int index, ReplySpec spec );

    @Override
    public Mono<Reply> add( final ReplySpec spec ) {

        return Mono.fromSupplier( () -> replies.isEmpty() && acked
                        ? spec.withPrivately( defaultPrivate )
                        : spec
                )
                .flatMap( s -> send( replies.size(), s ) )
                .doOnNext( replies::add )
                .doOnSuccess( s -> {
                    this.acked = true;
                } )
                .transform( sendLock::guard );

    }

    @Override
    public Reply get( final @NonNegative int index ) throws IndexOutOfBoundsException {

        return replies.get( index );

    }

    /**
     * Creates a new detached manager.
     *
     * @return The new manager.
     * @implSpec This method does not need to manage concurrency. This class ensures that
     *           each call to it only occurs while no other operations are ongoing.
     */
    @SideEffectFree
    protected abstract ReplyManager doDetach();

    /**
     * Checks that the current state is appropriate for detaching then creates a new
     * detached manager.
     *
     * @return The new manager.
     * @throws IllegalStateException If the state is not currently appropriate.
     */
    @SideEffectFree
    private ReplyManager doDetachValidate() throws IllegalStateException {

        if ( replies.isEmpty() ) {
            throw new IllegalStateException( "Cannot detach before sending a response" );
        }

        return doDetach();

    }

    @Override
    public Mono<ReplyManager> detach() {

        return Mono.fromSupplier( this::doDetachValidate )
                .transform( sendLock::guard );

    }
    
}
