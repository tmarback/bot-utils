package dev.sympho.bot_utils.event;

import discord4j.core.event.domain.message.MessageEvent;

/**
 * The context of a message event.
 * 
 * <p><b>Note that there is an expectation that the messages have a valid author.
 * Webhook messages are not supported and are expected to be filtered out.</b>
 *
 * @version 1.0
 * @since 1.0
 */
public interface MessageEventContext extends MessageBasedContext {

    @Override
    MessageEvent event();
    
}
