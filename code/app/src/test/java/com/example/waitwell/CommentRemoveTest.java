package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;

public class CommentRemoveTest {

    @Test
    public void adminCanDeleteAnyComment() {
        assertTrue(CommentUtils.canDelete("admin", "event123", "user456"));
        assertTrue(CommentUtils.canDelete("admin", "event999", "user999"));
    }

    @Test
    public void organizerCanDeleteCommentsOnOwnEventOnly() {
        String organizerId = "organizer1";
        String anotherOrganizerId = "organizer2";

        // Can delete comments on own event
        assertTrue(CommentUtils.canDelete("organizer", organizerId, organizerId));

        // Cannot delete comments on someone else's event
        assertFalse(CommentUtils.canDelete("organizer", anotherOrganizerId, organizerId));
    }

    @Test
    public void entrantCannotDeleteAnyComment() {
        assertFalse(CommentUtils.canDelete("entrant", "event123", "entrant1"));
    }

    @Test
    public void nullRoleOrUserCannotDelete() {
        assertFalse(CommentUtils.canDelete(null, "event123", "user1"));
        assertFalse(CommentUtils.canDelete("organizer", "event123", null));
    }
}