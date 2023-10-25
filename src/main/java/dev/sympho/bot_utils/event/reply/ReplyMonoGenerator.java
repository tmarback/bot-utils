package dev.sympho.bot_utils.event.reply;

import org.immutables.value.Value;

import discord4j.discordjson.MetaEncodingEnabled;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;

/**
 * Specification for creating a new event reply that can be directly subscribed to to execute.
 *
 * @version 1.0
 * @since 1.0
 */
@SpecStyle
@MetaEncodingEnabled
@Value.Immutable( builder = false )
@SuppressWarnings( { "immutables:subtype", "immutables:incompat" } )
abstract class ReplyMonoGenerator extends Mono<Reply> implements ReplySpecGenerator {

    /**
     * The backing reply manager.
     *
     * @return The manager.
     */
    abstract ReplyManager manager();

    @Override
    @SuppressWarnings( "argument" )
    public void subscribe( final CoreSubscriber<? super Reply> actual ) {
        manager().add( ReplySpec.copyOf( this ) ).subscribe( actual );
    }

    @Override
    public abstract String toString();
    
}
