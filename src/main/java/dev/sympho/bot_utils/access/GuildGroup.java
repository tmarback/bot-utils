package dev.sympho.bot_utils.access;

import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

/**
 * A group whose membership is defined at the guild level or higher (i.e. the channel is 
 * not taken into account).
 *
 * @version 1.0
 * @since 1.0
 * @apiNote Despite the name, implementations of this interface do not need to take the guild
 *          into account for determining membership; they only cannot use the channel.
 */
@FunctionalInterface
public interface GuildGroup extends Group {

    /**
     * Determines whether the user in the given context belongs to this group.
     *
     * @param context The access context being checked.
     * @return A Mono that issues {@code true} if the user belongs to this group under
     *         the context, or {@code false} otherwise.
     * @implSpec Implementations should prefer using the monos provided by the context directly
     *           instead of obtaining them through other means (such as through the client), as
     *           the context is optimized when possible to reuse existing instances instead of
     *           issuing new API calls.
     */
    @SideEffectFree
    Mono<Boolean> belongs( AccessContext context );

    /**
     * @implSpec Delegates to {@link #belongs(AccessContext)}.
     */
    @Override
    default Mono<Boolean> belongs( final ChannelAccessContext context ) {
        return belongs( ( AccessContext ) context );
    }

    /**
     * Determines whether the given user belongs to this group in the context of the 
     * given guild and channel.
     *
     * @param user The user to check for.
     * @param context The access context being checked.
     * @return A Mono that issues {@code true} if the user belongs to this group under
     *         the given context, or {@code false} otherwise.
     * @apiNote This is equivalent to using {@link AccessContext#asUser(User)} and
     *          delegating to {@link #belongs(AccessContext)}. The user in the given
     *          context is ignored.
     */
    @SideEffectFree
    default Mono<Boolean> belongs( final User user, final AccessContext context ) {

        return belongs( context.asUser( user ) );

    }
    
}
