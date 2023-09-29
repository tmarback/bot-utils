package dev.sympho.bot_utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.sympho.bot_utils.access.Group;
import dev.sympho.bot_utils.access.Groups;
import dev.sympho.bot_utils.access.NamedGroup;
import dev.sympho.bot_utils.component.ButtonManager;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

/**
 * Manager for displaying paginated data.
 * 
 * <p>Note that, in order for an instance of this class to begin operating, it must be registered
 * with a {@link ButtonManager} using the handler given by {@link #pageUpdateHandler()}.
 *
 * @version 1.0
 * @since 1.0
 */
public class PaginationManager {

    /** The base ID for an update button. */
    public static final String BUTTON_ID_UPDATE = "page-update";
    /** The ID for the counter display button. */
    public static final String BUTTON_ID_DISPLAY = "page-display";

    /** The label for the previous page button. */
    private static final ReactionEmoji BUTTON_LABEL_PREVIOUS = ReactionEmoji.unicode( "◀️" );
    /** The label for the next page button. */
    private static final ReactionEmoji BUTTON_LABEL_NEXT = ReactionEmoji.unicode( "▶️" );
    /** The label for the refresh page button. */
    private static final ReactionEmoji BUTTON_LABEL_REFRESH = ReactionEmoji.unicode( "🔁" );

    /** Separator character for arguments. */
    private static final String ARG_SEPARATOR = ":";

    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger( PaginationManager.class );

    /** The registered paginators. */
    private final Map<String, Paginator> paginators = new HashMap<>();

    /** Creates a new instance. */
    public PaginationManager() {}

    /**
     * Encodes page parameters into a button arg.
     *
     * @param id The page type ID.
     * @param index The page index.
     * @param args The page arguments. An empty string is treated as no arguments.
     * @return The encoded parameters.
     */
    private String encode( 
            final String id,
            final @NonNegative int index, 
            final String args 
    ) {

        final var pageStr = String.valueOf( index );
        return args.isEmpty() 
                ? String.join( ARG_SEPARATOR, id, pageStr )
                : String.join( ARG_SEPARATOR, id, pageStr, args );

    }

    /**
     * Decodes page parameters.
     *
     * @param arg The parameters encoded by {@link #encode(String, int, String)}.
     * @return The decoded parameters, in the order taken by {@link #encode(String, int, String)}.
     * @throws IllegalArgumentException If the given arg is not valid.
     */
    private Tuple3<String, @NonNegative Integer, String> decode( final String arg ) {

        final var s = arg.split( ARG_SEPARATOR, 3 );
        if ( s.length < 2 ) {
            throw new IllegalArgumentException( "Not a valid arg: " + arg );
        }

        final var id = s[0];
        final var index = Integer.parseInt( s[1] );
        final var args = s.length > 2 ? s[2] : "";

        if ( index < 0 ) {
            throw new IllegalArgumentException( "Arg has negative index" );
        }

        return Tuples.of( id, index, args );

    }

    /**
     * Registers a paginator.
     *
     * @param id The page type ID.
     * @param requiredGroup The group that a user must belong to to interact with
     *                      the page controls. If {@code null}, defaults to everyone.
     * @param generator The generator function for page data.
     * @return The paginator.
     * @throws IllegalArgumentException if there is already a registered paginator
     *                                  for the given type ID.
     */
    public synchronized Paginator register( 
            final String id, 
            final @Nullable Group requiredGroup,
            final PageGenerator generator 
    ) throws IllegalArgumentException {

        if ( paginators.containsKey( id ) ) {
            throw new IllegalArgumentException( "ID already in use" );
        }

        LOGGER.info( "Registering paginator {}", id );

        final var paginator = new Paginator( 
                generator, 
                id, 
                Objects.requireNonNullElse( requiredGroup, Groups.EVERYONE )
        );
        
        paginators.put( id, paginator );
        return paginator;

    }

    /**
     * Registers a paginator with no access restrictions.
     * 
     * <p>Equivalent to 
     * {@link #register(String, Group, PageGenerator) register(id, null, generator)}.
     * 
     *
     * @param id The page type ID.
     * @param generator The generator function for page data.
     * @return The paginator.
     * @throws IllegalArgumentException if there is already a registered paginator
     *                                  for the given type ID.
     */
    public Paginator register( 
            final String id,
            final PageGenerator generator 
    ) {

        return register( id, null, generator );

    }

    /**
     * Configures the handler for page update buttons. Must be registered with a
     * button manager for this pagination manager to work.
     *
     * @return The handler.
     */
    public ButtonManager.HandlerEntry pageUpdateHandler() {

        return ButtonManager.HandlerEntry.of( BUTTON_ID_UPDATE, ( ctx, arg ) -> {

            final var decoded = decode( arg );
            final String id = decoded.getT1();
            final int page = decoded.getT2();
            final String args = decoded.getT3();

            // Get associated paginator
            final var paginator = paginators.get( id );
            if ( paginator == null ) {
                return ctx.getEvent()
                        .reply( "Page type not recognized" )
                        .withEphemeral( true );
            }

            // Check membership to required group
            return paginator.requiredGroup.belongs( ctx )
                    .flatMap( b -> {

                        final var event = ctx.getEvent();

                        if ( b ) {
                            // Membership validated, update page
                            return paginator.generatePage( page, args )
                                    .flatMap( event::edit );
                        } else if ( paginator.requiredGroup instanceof NamedGroup g ) {
                            // Not member, indicate group name
                            return event.reply( String.format(
                                    "You must be part of the %s group to change the page.",
                                    g.name()
                            ) );
                        } else {
                            // Not member, no group name available
                            return event.reply( "You do not have permissions to change the page." );
                        }

                    } );

        }, true );

    }

    /**
     * The data of a page to be displayed.
     *
     * @param args The page arguments.
     * @param index The page index.
     * @param pageCount The total amount of pages available.
     * @param content The page content.
     * @param components The components to attach to the page. Only up to {@value #MAX_ROWS} 
     *                   rows as one row is used by the page controls.
     * @since 1.0
     */
    public record PageData(
            String args,
            @NonNegative int index,
            @NonNegative int pageCount,
            EmbedCreateSpec content,
            List<LayoutComponent> components
    ) {

        /** Maximum number of component rows. */
        public static final int MAX_ROWS = 4;

        /**
         * Creates a new instance.
         *
         * @throws IllegalArgumentException if more than {@value #MAX_ROWS} 
         *                                  component rows are given.
         */
        public PageData {

            if ( components.size() > MAX_ROWS ) {
                throw new IllegalArgumentException( "Too many component rows" );
            }

        }

        /**
         * Creates a new instance.
         *
         * @param args The page arguments.
         * @param index The page index.
         * @param pageCount The total amount of pages available.
         * @param content The page content.
         * @param components The components to attach to the page. Only up to {@value #MAX_ROWS} 
         *                   rows as one row is used by the page controls.
         * @return The created instance.
         */
        public static PageData of( 
                final String args,
                final @NonNegative int index,
                final @NonNegative int pageCount,
                final EmbedCreateSpec content, 
                final List<LayoutComponent> components 
        ) {

            return new PageData( args, index, pageCount, content, components );

        }

        /**
         * Creates a new instance.
         *
         * @param args The page arguments.
         * @param index The page index.
         * @param pageCount The total amount of pages available.
         * @param content The page content.
         * @return The created instance.
         */
        public static PageData of( 
                final String args,
                final @NonNegative int index,
                final @NonNegative int pageCount, 
                final EmbedCreateSpec content 
        ) {

            return of( args, index, pageCount, content, Collections.emptyList() );

        }

    }

    /**
     * A function that generates the contents of a page.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface PageGenerator {

        /**
         * Generates a page.
         *
         * @param index The page index.
         * @param args The page arguments.
         * @return The generated page.
         */
        Mono<PageData> generatePage( @NonNegative int index, String args );

    }

    /**
     * Entity responsible for generating content pages of a certain type.
     *
     * @since 1.0
     */
    public final class Paginator {

        /** The generator to use for page data. */
        private final PageGenerator generator;

        /** The page type ID. */
        private final String id;

        /** The required group for using page controls. */
        private final Group requiredGroup;

        /**
         * Creates a new instance.
         *
         * @param generator The generator to use for page data.
         * @param id The page type ID.
         * @param requiredGroup The required group for using page controls.
         */
        private Paginator( 
                final PageGenerator generator, 
                final String id, 
                final Group requiredGroup 
        ) {

            this.generator = generator;
            this.id = id;
            this.requiredGroup = requiredGroup;

        }

        /**
         * Renders a page.
         *
         * @param data The page data.
         * @return The rendered page.
         */
        private InteractionApplicationCommandCallbackSpec renderPage( final PageData data ) {

            LOGGER.trace( "Rendering page {}/{}/{}", id, data.args(), data.index() );

            final var buttonPrev = data.index() <= 0 // If first page
                    ? Button.primary( BUTTON_ID_UPDATE + "-", BUTTON_LABEL_PREVIOUS ).disabled()
                    : Button.primary( 
                            ButtonManager.makeId( 
                                    BUTTON_ID_UPDATE, 
                                    encode( id, data.index() - 1, data.args() ) 
                            ), 
                            BUTTON_LABEL_PREVIOUS 
                    );
            final var buttonNext = data.index() >= data.pageCount() - 1 // If last page
                    ? Button.primary( BUTTON_ID_UPDATE + "+", BUTTON_LABEL_NEXT ).disabled()
                    : Button.primary( 
                            ButtonManager.makeId( 
                                    BUTTON_ID_UPDATE, 
                                    encode( id, data.index() + 1, data.args() ) 
                            ), 
                            BUTTON_LABEL_NEXT 
                    );

            final var navBar = ActionRow.of(
                    buttonPrev,
                    Button.secondary( 
                            ButtonManager.makeId( 
                                    BUTTON_ID_UPDATE, 
                                    encode( id, data.index(), data.args() ) 
                            ), 
                            BUTTON_LABEL_REFRESH
                    ),
                    Button.secondary( 
                            BUTTON_ID_DISPLAY, 
                            "%d/%d".formatted( data.index() + 1, data.pageCount() ) 
                    ).disabled(),
                    buttonNext
            );

            return InteractionApplicationCommandCallbackSpec.builder()
                    .addEmbed( data.content() )
                    .addComponent( navBar )
                    .addAllComponents( data.components() )
                    .ephemeral( false )
                    .build();

        }

        /**
         * Generates a page.
         *
         * @param index The page index.
         * @param args The page arguments.
         * @return The generated page.
         * @throws IllegalArgumentException if the given index is negative.
         * 
         */
        private Mono<InteractionApplicationCommandCallbackSpec> generatePage( 
                final @NonNegative int index, final String args 
        ) throws IllegalArgumentException {

            if ( index < 0 ) {
                throw new IllegalArgumentException( "Negative indices not allowed" );
            }

            return generator.generatePage( index, args )
                    .map( this::renderPage );

        }

        /**
         * Generates the first page.
         * 
         * <p>Equivalent to {@link #generatePage(int, String) generatePage(0, args)}.
         *
         * @param args The page arguments.
         * @return The generated page.
         */
        public Mono<InteractionApplicationCommandCallbackSpec> generateFirst( final String args ) {

            return generatePage( 0, args );

        }

    }
    
}
