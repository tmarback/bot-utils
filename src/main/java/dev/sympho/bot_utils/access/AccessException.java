package dev.sympho.bot_utils.access;

import java.io.Serial;

/**
 * Exception that indicates that an access check failed.
 *
 * <p>Note that, to minimize the runtime overhead of using this exception
 * (as it is functionally just a result marker used to simplify control flow),
 * it does <i>not</i> record the stack trace or suppressed exceptions.
 *
 * @see AccessValidator#validate(GuildGroup)
 * @see ChannelAccessValidator#validate(Group)
 * @version 1.0
 * @since 1.0
 */
public final class AccessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5623938569744510032L;

    /**
     * The group that was required for access.
     */
    public final transient Group group;

    /**
     * Creates a new instance.
     *
     * @param group The group that was required for access.
     */
    public AccessException( final Group group ) {

        super( "Access denied", null, false, false );

        this.group = group;

    }
    
}
