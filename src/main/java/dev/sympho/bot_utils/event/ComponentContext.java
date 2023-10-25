package dev.sympho.bot_utils.event;

import org.checkerframework.checker.nullness.qual.Nullable;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

/**
 * The context of an event fired by interacting with a message component.
 *
 * @version 1.0
 * @since 1.0
 */
public interface ComponentContext extends DeferrableInteractionEventContext, MessageBasedContext {

    @Override
    ComponentInteractionEvent event();

    /**
     * @return The message, or {@code null} if the message is ephemeral.
     * @apiNote If the message is ephemeral, only {@link #messageId() the ID}
     *          will be present.
     */
    @Override
    default @Nullable Message message() {
        return event().getMessage().orElse( null );
    }

    @Override
    default Mono<Message> fetchMessage() {
        final var msg = message();
        return msg == null 
                ? Mono.empty()
                : msg.getClient().getMessageById( msg.getChannelId(), msg.getId() );
    }

    @Override
    default Snowflake messageId() {
        return event().getMessageId();
    }
    
}
