package dev.sympho.bot_utils.access;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Default group types and composition operations.
 *
 * @version 1.0
 * @since 1.0
 */
public final class Groups {

    /** The group of all Discord users (matches everyone). */
    public static final NamedGuildGroup EVERYONE = named( 
            ctx -> Mono.just( true ), 
            "Everyone" 
    );

    /** The empty group (matches nobody). */
    public static final NamedGuildGroup NOBODY = named( 
            ctx -> Mono.just( false ),
            "Nobody"
    );

    /**
     * The group of server admins (matches users with the 
     * {@link Permission#ADMINISTRATOR administrator} permission).
     */
    public static final NamedGuildGroup ADMINS = named(
            hasGuildPermissions( PermissionSet.of( Permission.ADMINISTRATOR ) ),
            "Administrators"
    );

    /** The group that only matches the server owner. */
    public static final NamedGuildGroup SERVER_OWNER = named(
            ctx -> ctx.guild()
                    .map( Guild::getOwnerId )
                    .map( ctx.user().getId()::equals ),
            "Server Owner"
    );

    /** The group that only matches the bot owner. */
    public static final NamedGuildGroup BOT_OWNER = named(
            ctx -> ctx.client().getApplicationInfo()
                    .map( ApplicationInfo::getOwnerId )
                    .map( ctx.user().getId()::equals ), 
            "Bot Owner" 
    );

    /** 
     * The group that matches a booster in the invoking server. 
     *
     * <p>Note that, unlike groups like {@link #ADMINS} or 
     * {@link #hasGuildPermissions(PermissionSet)}, this group will <i>never</i> match on
     * a private channel, as usually the goal is paywalling a feature rather than limiting
     * permissions.
     */
    public static final NamedGuildGroup BOOSTER = named(
            ctx -> ctx.member()
                    .map( Member::getPremiumTime )
                    .map( Optional::isPresent )
                    .defaultIfEmpty( false ),
            "Boosters"
    );

    /** Do not instantiate. */
    private Groups() {}

    /* Composition operations */

    /**
     * Adds a name to an existing group.
     *
     * @param group The group.
     * @param name The name.
     * @return The given group with the name set to the given name.
     */
    public static NamedGroup named( final Group group, final String name ) {

        return new Named( group, name );

    }

    /**
     * Adds a name to an existing group.
     *
     * @param group The group.
     * @param name The name.
     * @return The given group with the name set to the given name.
     */
    public static NamedGuildGroup named( final GuildGroup group, final String name ) {

        return new NamedGuild( group, name );

    }

    /**
     * Decorates a group so that membership is always checked in relation to the given guild,
     * rather than the guild where the command was executed in.
     * 
     * <p>In other words, this overrides the {@code guild} parameter provided to the
     * {@link Group#belongs(ChannelAccessContext)} method to the given guild, before delegating
     * to the given group.
     * 
     * <p>This allows for permission checking on commands that always operate on some remote
     * guild, and thus should be checked in relation to that guild.
     *
     * @param group The group.
     * @param guild The guild where group membership should be checked for. The issued value
     *              may change over time, but may <i>not</i> be empty.
     * @return The decorated group.
     */
    public static GuildGroup remote( final GuildGroup group, final Mono<Snowflake> guild ) {

        return new Remote( group, guild );

    }

    /**
     * Decorates a group so that membership is always checked in relation to the given guild,
     * rather than the guild where the command was executed in.
     * 
     * <p>In other words, this overrides the {@code guild} parameter provided to the
     * {@link Group#belongs(ChannelAccessContext)} method to the given guild, before delegating
     * to the given group.
     * 
     * <p>This allows for permission checking on commands that always operate on some remote
     * guild, and thus should be checked in relation to that guild.
     * 
     * <p>The {@link NamedGroup#name() name} of the group is passed through.
     *
     * @param group The group.
     * @param guild The guild where group membership should be checked for. The issued value
     *              may change over time, but may <i>not</i> be empty.
     * @return The decorated group.
     * @apiNote If the name of the group will be overriden, an explicit cast to the
     *          non-named variant may be used to avoid an unecessary wrapping layer:
     *          {@code named(remote((GuildGroup) group, guild), "some name")}
     */
    public static NamedGuildGroup remote( final NamedGuildGroup group, 
            final Mono<Snowflake> guild ) {

        return named( remote( ( GuildGroup ) group, guild ), group.name() );

    }

    /**
     * Decorates a group so that membership is always checked in relation to the given guild,
     * rather than the guild where the command was executed in.
     * 
     * <p>This allows for permission checking on commands that always operate on some remote
     * guild, and thus should be checked in relation to that guild.
     *
     * @param group The group.
     * @param guild The guild where group membership should be checked for. The issued value
     *              may change over time.
     * @return The decorated group.
     */
    public static GuildGroup remote( final GuildGroup group, final Supplier<Snowflake> guild ) {

        return remote( group, Mono.fromSupplier( guild ) );

    }

    /**
     * Decorates a group so that membership is always checked in relation to the given guild,
     * rather than the guild where the command was executed in.
     * 
     * <p>This allows for permission checking on commands that always operate on some remote
     * guild, and thus should be checked in relation to that guild.
     * 
     * <p>The {@link NamedGroup#name() name} of the group is passed through.
     *
     * @param group The group.
     * @param guild The guild where group membership should be checked for. The issued value
     *              may change over time.
     * @return The decorated group.
     * @apiNote If the name of the group will be overriden, an explicit cast to the
     *          non-named variant may be used to avoid an unecessary wrapping layer:
     *          {@code named(remote((GuildGroup) group, guild), "some name")}
     */
    public static NamedGuildGroup remote( final NamedGuildGroup group, 
            final Supplier<Snowflake> guild ) {

        return remote( group, Mono.fromSupplier( guild ) );

    }

    /**
     * Decorates a group so that membership is always checked in relation to the given guild,
     * rather than the guild where the command was executed in.
     * 
     * <p>This allows for permission checking on commands that always operate on some remote
     * guild, and thus should be checked in relation to that guild.
     *
     * @param group The group.
     * @param guild The guild where group membership should be checked for.
     * @return The decorated group.
     */
    public static GuildGroup remote( final GuildGroup group, final Snowflake guild ) {

        return remote( group, Mono.just( guild ) );

    }

    /**
     * Decorates a group so that membership is always checked in relation to the given guild,
     * rather than the guild where the command was executed in.
     * 
     * <p>This allows for permission checking on commands that always operate on some remote
     * guild, and thus should be checked in relation to that guild.
     * 
     * <p>The {@link NamedGroup#name() name} of the group is passed through.
     *
     * @param group The group.
     * @param guild The guild where group membership should be checked for.
     * @return The decorated group.
     * @apiNote If the name of the group will be overriden, an explicit cast to the
     *          non-named variant may be used to avoid an unecessary wrapping layer:
     *          {@code named(remote((Group) group, guild), "some name")}
     */
    public static NamedGuildGroup remote( final NamedGuildGroup group, final Snowflake guild ) {

        return remote( group, Mono.just( guild ) );

    }

    /* OR operator */

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose. The issued values may change over time 
     *               (between subscriptions).
     * @return The composed group.
     */
    public static Group any( final Flux<Group> groups ) {

        return ctx -> groups.flatMap( g -> g.belongs( ctx ) )
                .any( Boolean::booleanValue );

    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static Group any( final Stream<Group> groups ) {
        return any( Flux.fromStream( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static Group any( final Iterable<Group> groups ) {
        return any( Flux.fromIterable( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static Group any( final Group... groups ) {
        return any( Flux.fromArray( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup any( final GuildGroup... groups ) {
        return anyGuild( groups );
    }

    /* OR operator (guild version) */

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose. The issued values may change over time 
     *               (between subscriptions).
     * @return The composed group.
     */
    public static GuildGroup anyGuild( final Flux<GuildGroup> groups ) {

        return ctx -> groups.flatMap( g -> g.belongs( ctx ) )
                .any( Boolean::booleanValue );

    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup anyGuild( final Stream<GuildGroup> groups ) {
        return anyGuild( Flux.fromStream( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup anyGuild( final Iterable<GuildGroup> groups ) {
        return anyGuild( Flux.fromIterable( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>any</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup anyGuild( final GuildGroup... groups ) {
        return anyGuild( Flux.fromArray( groups ) );
    }

    /* AND operator */

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose. The issued values may change over time 
     *               (between subscriptions).
     * @return The composed group.
     */
    public static Group all( final Flux<Group> groups ) {

        return ctx -> groups.flatMap( g -> g.belongs( ctx ) )
                .all( Boolean::booleanValue );

    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static Group all( final Stream<Group> groups ) {
        return all( Flux.fromStream( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static Group all( final Iterable<Group> groups ) {
        return all( Flux.fromIterable( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static Group all( final Group... groups ) {
        return all( Flux.fromArray( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup all( final GuildGroup... groups ) {
        return allGuild( groups );
    }

    /* AND operator (guild version) */

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose. The issued values may change over time 
     *               (between subscriptions).
     * @return The composed group.
     */
    public static GuildGroup allGuild( final Flux<GuildGroup> groups ) {

        return ctx -> groups.flatMap( g -> g.belongs( ctx ) )
                .all( Boolean::booleanValue );

    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup allGuild( final Stream<GuildGroup> groups ) {
        return allGuild( Flux.fromStream( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup allGuild( final Iterable<GuildGroup> groups ) {
        return allGuild( Flux.fromIterable( groups ) );
    }

    /**
     * Composes multiple groups into a single group where a user is only a member
     * if they are a member of <b>all</b> of the given groups.
     *
     * @param groups The groups to compose.
     * @return The composed group.
     */
    public static GuildGroup allGuild( final GuildGroup... groups ) {
        return allGuild( Flux.fromArray( groups ) );
    }

    /* Default groups */

    /**
     * Creates a group composed of a single user.
     *
     * @param user The user. The issued value may change over time.
     * @return The group.
     */
    public static GuildGroup isUser( final Mono<Snowflake> user ) {

        return ctx -> user.map( ctx.user().getId()::equals );

    }

    /**
     * Creates a group composed of a single user.
     *
     * @param user The user. The issued value may change over time.
     * @return The group.
     */
    public static GuildGroup isUser( final Supplier<Snowflake> user ) {

        return isUser( Mono.fromSupplier( user ) );

    }

    /**
     * Creates a group composed of a single user.
     *
     * @param user The user.
     * @return The group.
     */
    public static GuildGroup isUser( final Snowflake user ) {

        return isUser( Mono.just( user ) );

    }

    /**
     * Creates a group composed of a set of users.
     *
     * @param users The users that belong to the group. The issued values may vary over
     *              time (between subscriptions).
     * @return The group.
     */
    public static GuildGroup inWhitelist( final Flux<Snowflake> users ) {

        return ctx -> users.any( ctx.user().getId()::equals );

    }

    /**
     * Creates a group composed of a set of users.
     *
     * @param users The users that belong to the group. The issued values may vary over
     *              time (between subscriptions).
     * @return The group.
     */
    public static GuildGroup inWhitelist( final Mono<? extends Collection<Snowflake>> users ) {

        return ctx -> users.map( u -> u.contains( ctx.user().getId() ) );

    }

    /**
     * Creates a group composed of a set of users.
     *
     * @param users The users that belong to the group. The issued values may vary over
     *              time (between requests).
     * @return The group.
     */
    public static GuildGroup inWhitelist( final Supplier<? extends Collection<Snowflake>> users ) {

        return inWhitelist( Mono.fromSupplier( users ) );

    }

    /**
     * Creates a group composed of a set of users.
     *
     * @param users The users that belong to the group.
     * @return The group.
     */
    public static GuildGroup inWhitelist( final Collection<Snowflake> users ) {

        final var allowed = Set.copyOf( users );
        return inWhitelist( Mono.just( allowed ) );

    }

    /**
     * Creates a group composed of a set of users.
     *
     * @param users The users that belong to the group.
     * @return The group.
     */
    public static GuildGroup inWhitelist( final Snowflake... users ) {
        return inWhitelist( Arrays.asList( users ) );
    }

    /**
     * Creates a group defined as all users that have the given role.
     *
     * @param role The role. The issued value may vary over time.
     * @return The group.
     */
    public static GuildGroup hasRole( final Mono<Snowflake> role ) {

        return ctx -> {
            
            final Flux<Snowflake> roles = ctx.member()
                    .flatMapMany( Member::getRoles )
                    .map( Role::getId );
            
            return role.flatMap( r -> roles.any( r::equals ) );

        };

    }

    /**
     * Creates a group defined as all users that have the given role.
     *
     * @param role The role. The issued value may vary over time.
     * @return The group.
     */
    public static GuildGroup hasRole( final Supplier<Snowflake> role ) {

        return hasRole( Mono.fromSupplier( role ) );

    }

    /**
     * Creates a group defined as all users that have the given role.
     *
     * @param role The role.
     * @return The group.
     */
    public static GuildGroup hasRole( final Snowflake role ) {

        return hasRole( Mono.just( role ) );

    }

    /**
     * Creates a group defined as all users that have <b>any</b> of the given roles.
     *
     * @param roles The roles. The issued values may vary over time (between subscriptions).
     * @return The group.
     */
    public static GuildGroup hasRolesAny( final Mono<? extends Collection<Snowflake>> roles ) {

        return ctx -> {
            
            final Flux<Snowflake> has = ctx.member()
                    .flatMapMany( Member::getRoles )
                    .map( Role::getId );

            return roles.flatMap( r -> has.any( r::contains ) );

        };

    }

    /**
     * Creates a group defined as all users that have <b>any</b> of the given roles.
     *
     * @param roles The roles. The issued values may vary over time (between subscriptions).
     * @return The group.
     */
    public static GuildGroup hasRolesAny( final Flux<Snowflake> roles ) {

        final var allowed = roles.collect( Collectors.toSet() );
        return hasRolesAny( allowed );

    }

    /**
     * Creates a group defined as all users that have <b>any</b> of the given roles.
     *
     * @param roles The roles. The issued values may vary over time.
     * @return The group.
     */
    public static GuildGroup hasRolesAny( final Supplier<? extends Collection<Snowflake>> roles ) {

        return hasRolesAny( Mono.fromSupplier( roles ) );

    }

    /**
     * Creates a group defined as all users that have <b>any</b> of the given roles.
     *
     * @param roles The roles.
     * @return The group.
     */
    public static GuildGroup hasRolesAny( final Collection<Snowflake> roles ) {

        final var allowed = Set.copyOf( roles );
        return hasRolesAny( Mono.just( allowed ) );

    }

    /**
     * Creates a group defined as all users that have <b>any</b> of the given roles.
     *
     * @param roles The roles.
     * @return The group.
     */
    public static GuildGroup hasRolesAny( final Snowflake... roles ) {
        return hasRolesAny( Arrays.asList( roles ) );
    }

    /**
     * Creates a group defined as all users that have <b>all</b> of the given roles.
     *
     * @param roles The roles. The issued values may vary over time (between subscriptions).
     * @return The group.
     */
    public static GuildGroup hasRolesAll( final Flux<Snowflake> roles ) {

        return ctx -> {
            
            final Mono<Set<Snowflake>> has = ctx.member()
                    .flatMapMany( Member::getRoles )
                    .map( Role::getId )
                    .collect( Collectors.toSet() );

            return has.flatMap( r -> roles.all( r::contains ) );

        };

    }

    /**
     * Creates a group defined as all users that have <b>all</b> of the given roles.
     *
     * @param roles The roles. The issued values may vary over time (between subscriptions).
     * @return The group.
     */
    public static GuildGroup hasRolesAll( final Mono<? extends Collection<Snowflake>> roles ) {

        return ctx -> {
            
            final Mono<Set<Snowflake>> has = ctx.member()
                    .flatMapMany( Member::getRoles )
                    .map( Role::getId )
                    .collect( Collectors.toSet() );

            return Mono.zip( has, roles )
                    .map( t -> t.getT1().containsAll( t.getT2() ) );

        };

    }

    /**
     * Creates a group defined as all users that have <b>all</b> of the given roles.
     *
     * @param roles The roles. The issued values may vary over time.
     * @return The group.
     */
    public static GuildGroup hasRolesAll( final Supplier<? extends Collection<Snowflake>> roles ) {

        return hasRolesAll( Mono.fromSupplier( roles ) );

    }

    /**
     * Creates a group defined as all users that have <b>all</b> of the given roles.
     *
     * @param roles The roles.
     * @return The group.
     */
    public static GuildGroup hasRolesAll( final Collection<Snowflake> roles ) {

        final var required = Set.copyOf( roles );
        return hasRolesAll( Mono.just( required ) );

    }

    /**
     * Creates a group defined as all users that have <b>all</b> of the given roles.
     *
     * @param roles The roles.
     * @return The group.
     */
    public static GuildGroup hasRolesAll( final Snowflake... roles ) {
        return hasRolesAll( Arrays.asList( roles ) );
    }

    /**
     * Creates a group defined as all users that have the given permissions in the guild.
     *
     * @param permissions The permissions. The issued value may vary over time.
     * @return The group.
     */
    public static GuildGroup hasGuildPermissions( final Mono<PermissionSet> permissions ) {

        return ctx -> ctx.member()
                .flatMap( Member::getBasePermissions )
                .flatMap( p -> permissions.map( p::containsAll ) )
                .defaultIfEmpty( true ); // Not in a guild

    }

    /**
     * Creates a group defined as all users that have the given permissions in the guild.
     *
     * @param permissions The permissions. The issued value may vary over time.
     * @return The group.
     */
    public static GuildGroup hasGuildPermissions( final Supplier<PermissionSet> permissions ) {

        return hasGuildPermissions( Mono.fromSupplier( permissions ) );

    }

    /**
     * Creates a group defined as all users that have the given permissions in the guild.
     *
     * @param permissions The permissions.
     * @return The group.
     */
    public static GuildGroup hasGuildPermissions( final PermissionSet permissions ) {

        return hasGuildPermissions( Mono.just( permissions ) );

    }

    /**
     * Creates a group defined as all users that have the given permissions in the channel.
     *
     * @param permissions The permissions. The issued value may vary over time.
     * @return The group.
     */
    public static Group hasChannelPermissions( final Mono<PermissionSet> permissions ) {

        return ctx -> ctx.channel()
                .ofType( GuildChannel.class )
                .flatMap( c -> c.getEffectivePermissions( ctx.user().getId() ) )
                .flatMap( p -> permissions.map( p::containsAll ) )
                .defaultIfEmpty( true ); // Not in a guild

    }

    /**
     * Creates a group defined as all users that have the given permissions in the channel.
     *
     * @param permissions The permissions. The issued value may vary over time.
     * @return The group.
     */
    public static Group hasChannelPermissions( final Supplier<PermissionSet> permissions ) {

        return hasChannelPermissions( Mono.fromSupplier( permissions ) );

    }

    /**
     * Creates a group defined as all users that have the given permissions in the channel.
     *
     * @param permissions The permissions.
     * @return The group.
     */
    public static Group hasChannelPermissions( final PermissionSet permissions ) {

        return hasChannelPermissions( Mono.just( permissions ) );

    }

    /* Inner classes */

    /**
     * Wrapper for a group that adds a name to it.
     *
     * @param group The wrapped group.
     * @param name The name of the group.
     * @version 1.0
     * @since 1.0
     */
    private record Named( Group group, String name ) implements NamedGroup {

        @Override
        public Mono<Boolean> belongs( final ChannelAccessContext context ) {

            return group.belongs( context );

        }

        @Override
        public String name() {

            return name;

        }

    }

    /**
     * Wrapper for a guild group that adds a name to it.
     *
     * @param group The wrapped group.
     * @param name The name of the group.
     * @version 1.0
     * @since 1.0
     */
    private record NamedGuild( GuildGroup group, String name ) implements NamedGuildGroup {

        @Override
        public Mono<Boolean> belongs( final AccessContext context ) {

            return group.belongs( context );

        }

        @Override
        public String name() {

            return name;

        }

    }

    /**
     * Wrapper for a group that overrides the {@code guild} parameter provided to the
     * {@link Group#belongs(Mono, Mono, User)} method to the given guild, before delegating
     * to the given group.
     *
     * @param group The group.
     * @param remoteGuild The guild where group membership should be checked for. The issued 
     *                    value may change over time.
     * @version 1.0
     * @since 1.0
     */
    private record Remote( GuildGroup group, Mono<Snowflake> remoteGuild ) implements GuildGroup {

        @Override
        public Mono<Boolean> belongs( final AccessContext context ) {

            return remoteGuild.map( context::asGuild )
                    .switchIfEmpty( Mono.error( () -> new IllegalStateException(
                            "No remote guild provided"
                    ) ) )
                    .flatMap( group::belongs );

        }

    }
    
}
