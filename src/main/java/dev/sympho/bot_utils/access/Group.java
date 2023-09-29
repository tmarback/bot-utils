package dev.sympho.bot_utils.access;

import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

/**
 * Specifies a group that users may belong to.
 * 
 * <p>Note that group membership is not static, but rather may change between guilds
 * and/or channels. This allows the definition of flexible permission groups that refer 
 * to different groups of people in different servers/channels.
 *
 * @version 1.0
 * @since 1.0
 */
@FunctionalInterface
public interface Group {
    
    /**
     * Determines whether the user in the given context belongs to this group.
     *
     * @param context The access context being checked.
     * @return A Mono that issues {@code true} if the user belongs to this group under
     *         the context, or {@code false} otherwise.
     * @implSpec Implementations should prefer using the monos provided by the context directly
     *           instead of obtaining them through other means (such as through the client), as
     *           the context is optimized when possible to reuse existing instances instead of
     *           issuing new API calls. Also note that the returned mono should <b>never</b> be
     *           empty.
     */
    @SideEffectFree
    Mono<Boolean> belongs( ChannelAccessContext context );

    /**
     * Determines whether the given user belongs to this group in the context of the 
     * given guild and channel.
     *
     * @param user The user to check for.
     * @param context The access context being checked.
     * @return A Mono that issues {@code true} if the user belongs to this group under
     *         the given context, or {@code false} otherwise.
     * @apiNote This is equivalent to using {@link ChannelAccessContext#asUser(User)} and
     *          delegating to {@link #belongs(ChannelAccessContext)}. The user in the given
     *          context is ignored.
     */
    @SideEffectFree
    default Mono<Boolean> belongs( final User user, final ChannelAccessContext context ) {

        return belongs( context.asUser( user ) );

    }

}
