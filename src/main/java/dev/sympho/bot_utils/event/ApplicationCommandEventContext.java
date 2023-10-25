package dev.sympho.bot_utils.event;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;

/**
 * The context of an application command interaction.
 *
 * @version 1.0
 * @since 1.0
 */
public interface ApplicationCommandEventContext extends DeferrableInteractionEventContext {

    @Override
    ApplicationCommandInteractionEvent event();
    
}
