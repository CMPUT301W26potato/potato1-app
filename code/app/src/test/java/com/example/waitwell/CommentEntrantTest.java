package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;
import com.google.firebase.Timestamp;
import java.util.Calendar;

/**
 * Unit tests for {@link CommentUtils#canEntrantComment(String, Timestamp)}.
 * Checks if entrants are allowed to comment based on role and registration status.
 * @author Sarang Kim
 */
public class CommentEntrantTest {

    /** Entrants can comment if registration is still open. */
    @Test
    public void entrantCanCommentIfRegistrationOpen() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1); // 1 day in the future
        Timestamp close = new Timestamp(cal.getTime());

        assertTrue(CommentUtils.canEntrantComment("entrant", close));
    }

    /** Entrants cannot comment if registration has closed. */
    @Test
    public void entrantCannotCommentIfRegistrationClosed() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1); // 1 day in the past
        Timestamp close = new Timestamp(cal.getTime());

        assertFalse(CommentUtils.canEntrantComment("entrant", close));
    }

    /** Non-entrant roles cannot comment regardless of registration status. */
    @Test
    public void nonEntrantCannotComment() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp close = new Timestamp(cal.getTime());

        assertFalse(CommentUtils.canEntrantComment("organizer", close));
        assertFalse(CommentUtils.canEntrantComment("admin", close));
        assertFalse(CommentUtils.canEntrantComment(null, close));
    }
}