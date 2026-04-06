package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link CommentUtils#canDelete(String, String, String)}.
 * Verifies which roles can delete comments based on role and event ownership.
 * Author: Sarang
 */
public class CommentRemoveTest {

    /** Admins can delete any comment. */
    @Test
    public void adminCanDeleteAnyComment() {
        assertTrue(CommentUtils.canDelete("admin", "event123", "user456"));
        assertTrue(CommentUtils.canDelete("admin", "event999", "user999"));
    }

    /** Organizers can delete comments only on their own events. */
    @Test
    public void organizerCanDeleteCommentsOnOwnEventOnly() {
        String organizerId = "organizer1";
        String anotherOrganizerId = "organizer2";

        //can delete comment on own event
        assertTrue(CommentUtils.canDelete("organizer", organizerId, organizerId));
        //test if can delete comment on elses event
        assertFalse(CommentUtils.canDelete("organizer", anotherOrganizerId, organizerId));
    }

    /** Entrants cannot delete any comments. */
    @Test
    public void entrantCannotDeleteAnyComment() {
        assertFalse(CommentUtils.canDelete("entrant", "event123", "entrant1"));
    }

    /** Null role or user cannot delete comments. */
    @Test
    public void nullRoleOrUserCannotDelete() {
        assertFalse(CommentUtils.canDelete(null, "event123", "user1"));
        assertFalse(CommentUtils.canDelete("organizer", "event123", null));
    }
}