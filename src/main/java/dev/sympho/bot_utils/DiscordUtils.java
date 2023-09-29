package dev.sympho.bot_utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.apache.commons.io.input.AutoCloseInputStream;
import org.checkerframework.dataflow.qual.SideEffectFree;

import discord4j.common.util.Snowflake;
import discord4j.core.object.Embed;
import discord4j.core.object.Embed.Image;
import discord4j.core.object.Embed.Thumbnail;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields.File;
import discord4j.core.spec.MessageCreateFields.FileSpoiler;
import discord4j.discordjson.possible.Possible;

/**
 * Provides assorted utilities for interacting with Discord.
 *
 * @version 1.0
 * @since 1.0
 */
public final class DiscordUtils {

    /** Do not instantiate. */
    private DiscordUtils() {}

    /**
     * Converts an {@link Optional} into a {@link Possible}.
     *
     * @param <T> The contained object type.
     * @param o The optional.
     * @return The possible.
     */
    @SuppressWarnings( "optional.parameter" )
    @SideEffectFree
    public static <T> Possible<T> toPossible( final Optional<T> o ) {

        if ( o.isPresent() ) {
            return Possible.of( o.get() );
        } else {
            return Possible.absent();
        }

    }

    /**
     * Creates an embed spec from an embed.
     *
     * @param embed The embed.
     * @return The equivalent spec.
     */
    @SideEffectFree
    public static EmbedCreateSpec toSpec( final Embed embed ) {

        final var builder = EmbedCreateSpec.builder()
                .title( toPossible( embed.getTitle() ) )
                .description( toPossible( embed.getDescription() ) )
                .thumbnail( toPossible( embed.getThumbnail().map( Thumbnail::getUrl ) ) )
                .image( toPossible( embed.getImage().map( Image::getUrl ) ) )
                .color( toPossible( embed.getColor() ) );

        if ( embed.getAuthor().isPresent() ) {
            final var author = embed.getAuthor().get();
            builder.author( 
                    author.getName().orElse( "" ), 
                    author.getUrl().orElse( null ), 
                    author.getIconUrl().orElse( null )
            );
        }

        if ( embed.getFooter().isPresent() ) {
            final var footer = embed.getFooter().get();
            builder.footer(
                    footer.getText(), 
                    footer.getIconUrl().orElse( null )
            );
        }

        for ( final var field : embed.getFields() ) {

            builder.addField( field.getName(), field.getValue(), field.isInline() );

        }

        return builder.build();

    }

    /**
     * Constructs a mention.
     *
     * @param prefix The mention prefix.
     * @param id The ID to mention.
     * @return The mention.
     */
    @SideEffectFree
    private static String mention( final String prefix, final Snowflake id ) {

        return String.join( "",
            "<", prefix, id.asString(), ">"
        );

    }

    /**
     * Constructs a mention to a user.
     *
     * @param id The ID of the user to mention.
     * @return The mention.
     */
    @SideEffectFree
    public static String mentionUser( final Snowflake id ) {

        return mention( "@", id );

    }

    /**
     * Constructs a mention to a role.
     *
     * @param id The ID of the role to mention.
     * @return The mention.
     */
    @SideEffectFree
    public static String mentionRole( final Snowflake id ) {

        return mention( "@&", id );

    }

    /**
     * Constructs a mention to a channel.
     *
     * @param id The ID of the channel to mention.
     * @return The mention.
     */
    @SideEffectFree
    public static String mentionChannel( final Snowflake id ) {

        return mention( "#", id );

    }

    /**
     * Prepares data to be attached to a message as a file.
     *
     * @param filename The filename.
     * @param data The file data.
     * @param spoiler Whether to mark the file as a spoiler.
     * @return The prepared attachment.
     * @throws AttachmentException if an error occurs.
     */
    @SideEffectFree
    public static File attachData( final String filename, final byte[] data, 
            final boolean spoiler ) throws AttachmentException {

        try {
            final var stream = AutoCloseInputStream.builder()
                    .setByteArray( data )
                    .get();

            return spoiler ? FileSpoiler.of( filename, stream ) : File.of( filename, stream );
        } catch ( final IOException ex ) {
            throw new AttachmentException( "Could not attach file data", ex );
        }

    }

    /**
     * Prepares an image to be attached to a message.
     *
     * @param name The image name (filename without extension).
     * @param image The image.
     * @param spoiler Whether to mark the image as a spoiler.
     * @return The prepared attachment.
     * @throws AttachmentException if an error occurs.
     */
    @SideEffectFree
    public static File attachImage( final String name, final BufferedImage image, 
            final boolean spoiler ) throws AttachmentException {

        try ( var os = new ByteArrayOutputStream() ) {
            ImageIO.write( image, "png", os );

            return attachData( name + ".png", os.toByteArray(), spoiler );
        } catch ( final IOException ex ) {
            throw new AttachmentException( "Could not write image", ex );
        }

    }

    /**
     * Exception that represents an error encountered while creating an attachment.
     *
     * @since 1.0
     */
    public static class AttachmentException extends RuntimeException {

        private static final long serialVersionUID = -1570577395324066698L;

        /**
         * Creates a new instance.
         *
         * @param message The error message.
         * @param cause The underlying cause.
         */
        public AttachmentException( final String message, final Throwable cause ) {
            super( message, cause );
        }

    }
    
}
