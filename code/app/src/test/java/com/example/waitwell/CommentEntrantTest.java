package com.example.waitwell;

import org.junit.Test;
import static org.junit.Assert.*;
import com.google.firebase.Timestamp;
import java.util.Calendar;

public class CommentEntrantTest {

    @Test
    public void entrantCanCommentIfRegistrationOpen() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1); // 1 day in the future
        Timestamp close = new Timestamp(cal.getTime());

        assertTrue(CommentUtils.canEntrantComment("entrant", close));
    }

    @Test
    public void entrantCannotCommentIfRegistrationClosed() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1); // 1 day in the past
        Timestamp close = new Timestamp(cal.getTime());

        assertFalse(CommentUtils.canEntrantComment("entrant", close));
    }

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