package dev.sympho.bot_utils.event;

import java.util.Collection;

import discord4j.core.event.domain.interaction.ModalSubmitInteractionEvent;
import discord4j.core.object.component.TextInput;

/**
 * The context of a modal component being submitted by a user.
 *
 * @version 1.0
 * @since 1.0
 */
public interface ModalContext extends ComponentContext {

    @Override
    ModalSubmitInteractionEvent event();

    /**
     * Retrieves the input field with the given custom ID.
     *
     * @param fieldId The custom ID.
     * @return The input field with that ID.
     * @throws IllegalArgumentException if there is no field in this context with that ID.
     */
    TextInput getField( final String fieldId ) throws IllegalArgumentException;

    /**
     * Retrieves the input fields in this context.
     *
     * @return The input fields.
     */
    Collection<TextInput> getFields();
    
}
