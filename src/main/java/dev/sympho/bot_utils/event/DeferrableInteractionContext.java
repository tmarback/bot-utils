package dev.sympho.bot_utils.event;

import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;

/**
 * The context of an interaction-based event that is deferrable.
 *
 * @version 1.0
 * @since 1.0
 */
public interface DeferrableInteractionContext extends InteractionContext, RepliableContext {

    @Override
    DeferrableInteractionEvent event();
    
}
