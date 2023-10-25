package dev.sympho.bot_utils.event.reply;

import org.immutables.value.Value;

import discord4j.core.object.entity.Message;
import discord4j.discordjson.MetaEncodingEnabled;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

/**
 * Specification for editing an event reply that can be directly subscribed to to execute.
 *
 * @version 1.0
 * @since 1.0
 */
@SpecStyle
@MetaEncodingEnabled
@Value.Immutable( builder = false )
@SuppressWarnings( { "immutables:subtype", "immutables:incompat" } )
abstract class ReplyEditMonoGenerator extends Mono<Message> 
        implements ReplyEditSpecGenerator {

    /**
     * The backing reply.
     *
     * @return The reply.
     */
    abstract Reply reply();

    @Override
    @SuppressWarnings( "argument" )
    public void subscribe( final CoreSubscriber<? super Message> actual ) {
        reply().edit( ReplyEditSpec.copyOf( this ) ).subscribe( actual );
    }

    @Override
    public abstract String toString();
    
}
