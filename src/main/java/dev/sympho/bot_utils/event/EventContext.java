package dev.sympho.bot_utils.event;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

import dev.sympho.bot_utils.access.AccessContext;
import dev.sympho.bot_utils.access.AccessValidator;
import dev.sympho.bot_utils.access.GuildGroup;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

/**
 * The context of an user-triggered event received from Discord.
 *
 * @version 1.0
 * @since 1.0
 * @implSpec A context must be effectively constant; that is, an implementation of this interface
 *           must always return the same value on methods that return a direct value (such as
 *           {@link #user()} or {@link #guildId()}). Methods that fetch remote resources
 *           (i.e. that return a Mono) may return different objects over time (as the remote object
 *           is modified), but must always reference the same entity.
 */
public interface EventContext extends AccessContext, AccessValidator {

    /**
     * Retrieves the triggering event.
     *
     * @return The event.
     */
    Event event();

    @Override
    default GatewayDiscordClient client() {
        return event().getClient();
    }

    /**
     * The access validator used in this context.
     *
     * @return The validator.
     */
    @Pure
    AccessValidator validator();

    /**
     * @see #validator()
     * @see AccessValidator#hasAccess(GuildGroup)
     * @apiNote This is a convenience shortcut for using the {@link #validator() context validator}.
     *          It is equivalent to {@code validator().hasAccess(group)}.
     */
    @Override
    default Mono<Boolean> hasAccess( final GuildGroup group ) {

        return validator().hasAccess( group );

    }

    /**
     * @see #validator()
     * @see AccessValidator#validate(GuildGroup)
     * @apiNote This is a convenience shortcut for using the {@link #validator() context validator}.
     *          It is equivalent to {@code validator().validate(group)}.
     */
    @Override
    default Mono<Void> validate( final GuildGroup group ) {
        
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
    default Mono<Boolean> belongs( final User user, final GuildGroup group ) {

        return group.belongs( user, this )
                .defaultIfEmpty( false ); // Just to be safe

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
    default Mono<Boolean> belongs( final Snowflake user, final GuildGroup group ) {

        return client().getUserById( user ).flatMap( u -> belongs( u, group ) );

    }
    
}
