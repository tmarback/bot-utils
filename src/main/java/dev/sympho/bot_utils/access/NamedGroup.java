package dev.sympho.bot_utils.access;

import org.checkerframework.dataflow.qual.Pure;

/**
 * A group that also has a name.
 *
 * @version 1.0
 * @since 1.0
 * @apiNote This optional interface does not change the functionality of a Group in any way.
 *          It only provides extra context for error messages and logs.
 */
public interface NamedGroup extends Group {

    /**
     * Retrieves the name of the group.
     *
     * @return The group's name.
     */
    @Pure
    String name();
    
}
