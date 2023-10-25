package dev.sympho.bot_utils.event;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;

/**
 * The context of a button component being pressed by a user.
 *
 * @version 1.0
 * @since 1.0
 */
public interface ButtonEventContext extends ComponentContext {

    @Override
    ButtonInteractionEvent event();
    
}
