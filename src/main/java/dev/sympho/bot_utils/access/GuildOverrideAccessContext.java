package dev.sympho.bot_utils.access;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.util.NullnessUtil;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

/**
 * An access context that copies an external context but overrides the guild 
 * (and associated values).
 *
 * @version 1.0
 * @since 1.0
 */
class GuildOverrideAccessContext implements AccessContext {

    /** The original context. */
    private final AccessContext base;
    /** The guild to override to. */
    private final @Nullable Snowflake guild;

    /**
     * Creates a new instance.
     *
     * @param base The original context.
     * @param guild The guild to override to. May be {@code null} to set as a 
     *              private-channel context.
     */
    GuildOverrideAccessContext( final AccessContext base, final @Nullable Snowflake guild ) {

        this.base = base;
        this.guild = guild;

    }

    @Override
    public GatewayDiscordClient getClient() {
        return base.getClient();
    }

    @Override
    public User getUser() {
        return base.getUser();
    }

    @Override
    public Mono<Guild> getGuild() {
        return guild == null 
                ? Mono.empty() 
                : base.getClient()
                        .getGuildById( guild )
                        .switchIfEmpty( Mono.error( () -> new IllegalArgumentException( 
                                // IDK why the refinement thinks guild is nullable by this point
                                "Guild %s not found".formatted( NullnessUtil.castNonNull( guild ) )
                        ) ) );
    }

    @Override
    public @Nullable Snowflake getGuildId() {
        return guild;
    }

    @Override
    public boolean isPrivate() {
        return guild == null;
    }

}
