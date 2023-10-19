package dev.sympho.bot_utils.access;

import org.checkerframework.dataflow.qual.SideEffectFree;

import reactor.core.publisher.Mono;

/**
 * Validator that determines whether a caller has certain access rights in the
 * context of a channel-based event.
 *
 * @version 1.0
 * @since 1.0
 * @apiNote This type should usually not be manually created during handling;
 *          Rather, it should be obtained from the execution context in order to respect
 *          current configuration.
 */
@FunctionalInterface
public interface ChannelAccessValidator extends AccessValidator {

    /**
     * Determines whether the invoking user in the current execution context has access 
     * equivalent to the given group.
     * 
     * <p>Note that while the most straightforward implementation of this interface is
     * to simply check if the caller {@link Group#belongs(ChannelAccessContext) belongs}
     * to the given group, implementations are allowed to add other conditions under
     * which a user has equivalent permissions despite not belonging to the group
     * (or conversely does <i>not</i> have permissions despite <i>belonging</i> to
     * the group).
     *
     * @param group The group required for access.
     * @return Whether the caller has access equivalent to the given group under the 
     *         current execution context.
     * @implSpec The returned mono should <b>never</b> be empty. However, client code
     *           is strongly encouraged to consider an empty result as equivalent to
     *           {@code false} to minimize the chance of a bug causing an invalid access
     *           to be allowed (fail-closed).
     */
    @SideEffectFree
    Mono<Boolean> hasAccess( Group group );

    @Override
    default Mono<Boolean> hasAccess( final GuildGroup group ) {

        return hasAccess( ( Group ) group );

    }

    /**
     * Determines whether the invoking user in the current execution context
     * has access equivalent to the given group.
     *
     * @param group The group required for access.
     * @return A Mono that completes empty if the caller has access equivalent to the given
     *         group under the current execution context, or otherwise issues an error of type
     *         {@link AccessException}.
     * @apiNote This is a convenience method for scenarios where a failed check interrupts
     *          execution immediately.
     * @implSpec If the {@link #hasAccess(Group) access check} returns an empty result 
     *           (despite being a breach of the API), it is assumed to be a failed check as a 
     *           safety measure (fails-closed).
     */
    @SideEffectFree
    default Mono<Void> validate( final Group group ) {

        return AccessValidator.validateResult( hasAccess( group ), group );

    }
    
}
