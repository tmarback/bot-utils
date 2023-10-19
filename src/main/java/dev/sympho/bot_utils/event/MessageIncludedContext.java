package dev.sympho.bot_utils.event;

import discord4j.core.object.entity.Message;

/**
 * The context of an event that is related to a message and <b>analwaysd</b> includes the 
 * message data as part of the event payload.
 *
 * @version 1.0
 * @since 1.0
 */
public interface MessageIncludedContext extends MessageBasedContext {

    /**
     * @return The message.
     */
    @Override
    Message message();
    
}
