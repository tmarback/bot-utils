package dev.sympho.bot_utils.access;

import java.util.function.Supplier;

import org.checkerframework.dataflow.qual.SideEffectFree;

import reactor.core.publisher.Mono;

/**
 * A manager that provides access validators for different execution contexts.
 * 
 * <p>The {@link #basic() most basic} manager creates validators that simply check if the
 * user belongs to the given group; however, manager implementations are free to customize
 * access control by checking additional context information, external state, or even
 * dynamically applying different validation rules depending on the context.
 *
 * @version 1.0
 * @since 1.0
 */
@FunctionalInterface
public interface AccessManager {

    /**
     * Validates that an access is legal.
     *
     * @param context The access context.
     * @param group The required group.
     * @param membershipCheck A function that applies {@link Group#belongs(ChannelAccessContext)}
     *                        or {@link GuildGroup#belongs(AccessContext)} (as appropriate).
     * @return Whether the access is allowed.
     */
    Mono<Boolean> doValidate( 
            AccessContext context, 
            Group group, 
            Supplier<Mono<Boolean>> membershipCheck 
    );

    /**
     * Creates an access validator under the given context.
     *
     * @param context The access context for the current execution.
     * @return The appropriate access validator.
     */
    default ChannelAccessValidator validator( final ChannelAccessContext context ) {
        return group -> doValidate( context, group, () -> group.belongs( context ) );
    }

    /**
     * Creates an access validator under the given context.
     *
     * @param context The access context for the current execution.
     * @return The appropriate access validator.
     */
    default AccessValidator validator( final AccessContext context ) {
        return group -> doValidate( context, group, () -> group.belongs( context ) );
    }

    /**
     * Creates a manager for which all validators always allow access.
     * That is, a manager that allows access for any user to any group (effectively disabling
     * group checking).
     *
     * @return The manager.
     */
    @SideEffectFree
    static AccessManager alwaysAllow() {

        return ( ctx, g, check ) -> Mono.just( true );

    }

    /**
     * Creates a manager for which all validators always deny access.
     * That is, a manager that denies access for any user to any group (effectively disabling
     * any functionality that requires group membership).
     *
     * @return The manager.
     */
    @SideEffectFree
    static AccessManager alwaysDeny() {

        return ( ctx, g, check ) -> Mono.just( false );

    }

    /**
     * Creates a manager that issues validators that perform simple group memebership checks
     * (that is, a user has equivalent access to a group if and only if they belong to that
     * group).
     *
     * @return The manager.
     */
    @SideEffectFree
    static AccessManager basic() {

        return ( ctx, g, check ) -> check.get();

    }

    /**
     * Creates a manager that issues validators that generally perform group membership checks,
     * but also allows a given group to override any permission in the system (that is, users
     * that are members of the given group have equivalent permissions to any other group).
     *
     * @param overrideGroup The group that overrides any permission check.
     * @return The manager.
     */
    @SideEffectFree
    static AccessManager overridable( final GuildGroup overrideGroup ) {

        return ( ctx, g, check ) -> check.get()
                .filter( Boolean::booleanValue ) // Make empty if not allowed
                .switchIfEmpty( Mono.defer( // If not allowed, check override
                        () -> overrideGroup.belongs( ctx ) 
                ) )
                .defaultIfEmpty( false );

    }
    
}
