package dev.sympho.bot_utils.event.reply;

import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.InteractionFollowupCreateSpec;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;

/**
 * Utilities for creating reply specification instances.
 *
 * @version 1.0
 * @since 1.0
 */
public final class ReplyUtils {

    /** Do not instantiate. */
    private ReplyUtils() {}

    /**
     * Creates a reply spec from a message create spec.
     * 
     * <p>The {@link CommandReplySpec#privately()} field is set to absent.
     *
     * @param spec The source spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplySpec from( final MessageCreateSpec spec ) {

        return ReplySpecGenerator.from( spec );

    }

    /**
     * Creates a reply spec from an interaction reply spec.
     * 
     * <p>The {@link CommandReplySpec#privately()} field is set to 
     * {@link InteractionApplicationCommandCallbackSpec#ephemeral()}.
     *
     * @param spec The source spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplySpec from( final InteractionApplicationCommandCallbackSpec spec ) {

        return ReplySpecGenerator.from( spec );

    }

    /**
     * Creates a reply spec from an interaction followup spec.
     * 
     * <p>The {@link CommandReplySpec#privately()} field is set to 
     * {@link InteractionFollowupCreateSpec#ephemeral()}.
     *
     * @param spec The source spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplySpec from( final InteractionFollowupCreateSpec spec ) {

        return ReplySpecGenerator.from( spec );

    }

    /**
     * Creates an edit spec from a message edit spec.
     *
     * @param spec The source spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplyEditSpec from( final MessageEditSpec spec ) {

        return ReplyEditSpecGenerator.from( spec );

    }

    /**
     * Creates an edit spec from an interaction reply edit spec.
     *
     * @param spec The source spec.
     * @return The converted spec.
     */
    @SideEffectFree
    static ReplyEditSpec from( final InteractionReplyEditSpec spec ) {

        return ReplyEditSpecGenerator.from( spec );

    }
    
}
