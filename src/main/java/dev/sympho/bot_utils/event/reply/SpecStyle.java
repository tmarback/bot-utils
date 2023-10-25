package dev.sympho.bot_utils.event.reply;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.immutables.value.Value;

/**
 * Immutables style that matches the one used by Discord4J specs.
 *
 * @version 1.0
 * @since 1.0
 */
// BEGIN LONG LINES
// https://github.com/Discord4J/Discord4J/blob/master/core/src/main/java/discord4j/core/spec/SpecStyle.java
// END LONG LINES
@Target( { ElementType.PACKAGE, ElementType.TYPE } )
@Retention( RetentionPolicy.CLASS )
@Value.Style(
        typeAbstract = "*Generator",
        typeImmutable = "*",
        visibility = Value.Style.ImplementationVisibility.PUBLIC,
        deepImmutablesDetection = true,
        allMandatoryParameters = true,
        depluralize = true,
        instance = "create"
)
public @interface SpecStyle {}
