package dev.sympho.bot_utils.event;

import org.checkerframework.dataflow.qual.SideEffectFree;

import dev.sympho.bot_utils.access.ChannelAccessContext;
import dev.sympho.bot_utils.access.ChannelAccessValidator;
import dev.sympho.bot_utils.access.Group;
import dev.sympho.bot_utils.access.GuildGroup;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

/**
 * The context of an user-triggered event that is bound to a specific channel.
 *
 * @version 1.0
 * @since 1.0
 */
public interface ChannelEventContext 
        extends EventContext, ChannelAccessContext, ChannelAccessValidator {

    @Override
    ChannelAccessValidator validator();

    /**
     * @see #validator()
     * @see ChannelAccessValidator#hasAccess(Group)
     * @apiNote This is a convenience shortcut for using the {@link #validator() context validator}.
     *          It is equivalent to {@code validator().hasAccess(group)}.
     */
    @Override
    default Mono<Boolean> hasAccess( final Group group ) {

        return validator().hasAccess( group );

    }

    @Override
    default Mono<Boolean> hasAccess( final GuildGroup group ) {

        return validator().hasAccess( group );

    }

    /**
     * @see #validator()
     * @see ChannelAccessValidator#validate(Group)
     * @apiNote This is a convenience shortcut for using the {@link #validator() context validator}.
     *          It is equivalent to {@code validator().validate(group)}.
     */
    @Override
    default Mono<Void> validate( final Group group ) {
        
        return validator().validate( group );

    }

    /**
     * Determines whether the given user belongs to the given group in the context of
     * this event.
     *
     * @param user The user to check for.
     * @param group The group to check for.
     * @return A Mono that emits {@code true} if the given user belongs to the given
     *         group under this event context, or {@code false} otherwise.
     * @apiNote This is a convenience method equivalent to {@code group.belongs(user, this)}.
     */
    @SideEffectFree
    default Mono<Boolean> belongs( final User user, final Group group ) {

        return group.belongs( user, this )
                .defaultIfEmpty( false ); // Just to be safe

    }

    @Override
    default Mono<Boolean> belongs( final User user, final GuildGroup group ) {

        return belongs( user, ( Group ) group );

    }

    /**
     * Determines whether the given user belongs to the given group in the context of
     * this event.
     *
     * @param user The ID of the user to check for.
     * @param group The group to check for.
     * @return A Mono that emits {@code true} if the given user belongs to the given
     *         group under this event context, or {@code false} otherwise.
     * @apiNote This is a convenience method equivalent to fetching the user then invoking
     *          {@code group.belongs(user, this)}.
     */
    @SideEffectFree
    default Mono<Boolean> belongs( final Snowflake user, final Group group ) {

        return client().getUserById( user ).flatMap( u -> belongs( u, group ) );

    }

    @Override
    default Mono<Boolean> belongs( final Snowflake user, final GuildGroup group ) {

        return belongs( user, ( Group ) group );

    }
    
}
