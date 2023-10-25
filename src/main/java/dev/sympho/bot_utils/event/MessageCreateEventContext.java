package dev.sympho.bot_utils.event;

import org.checkerframework.checker.nullness.qual.Nullable;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.core.publisher.Mono;

/**
 * The context of a new message being created.
 *
 * @version 1.0
 * @since 1.0
 */
public interface MessageCreateEventContext extends MessageEventContext, MessageIncludedContext {

    @Override
    MessageCreateEvent event();

    @Override
    default Message message() {
        return event().getMessage();
    }

    @Override
    default Mono<Message> fetchMessage() {
        return client().getMessageById( channelId(), messageId() );
    }

    @Override
    default Snowflake messageId() {
        return event().getMessage().getId();
    }

    @Override
    default User user() {
        return event().getMessage().getAuthor()
                .orElseThrow( () -> new IllegalStateException( "Webhook messages not supported" ) );
    }

    @Override
    default Mono<Guild> guild() {
        return event().getGuild();
    }

    @Override
    default @Nullable Snowflake guildId() {
        return event().getGuildId().orElse( null );
    }

    @Override
    default Snowflake channelId() {
        return event().getMessage().getChannelId();
    }

    @Override
    default Mono<MessageChannel> channel() {
        return event().getMessage().getChannel();
    }
    
}
