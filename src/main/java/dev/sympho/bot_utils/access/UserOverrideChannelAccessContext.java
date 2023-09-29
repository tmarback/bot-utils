package dev.sympho.bot_utils.access;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.Channel;
import reactor.core.publisher.Mono;

/**
 * A channel access context that copies an external context but overrides the user 
 * (and associated values).
 *
 * @version 1.0
 * @since 1.0
 */
class UserOverrideChannelAccessContext extends UserOverrideAccessContext<ChannelAccessContext> 
        implements ChannelAccessContext {

    /**
     * Creates a new instance.
     *
     * @param base The original context.
     * @param user The user to override to.
     */
    UserOverrideChannelAccessContext( final ChannelAccessContext base, final User user ) {
        super( base, user );
    }

    @Override
    public Mono<? extends Channel> getChannel() {
        return base.getChannel();
    }

    @Override
    public Snowflake getChannelId() {
        return base.getChannelId();
    }
    
}
