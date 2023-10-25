package dev.sympho.bot_utils.event;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;

/**
 * The context of a slash command interaction.
 *
 * @version 1.0
 * @since 1.0
 */
public interface SlashCommandEventContext extends ApplicationCommandEventContext {

    @Override
    ChatInputInteractionEvent event();
    
}
