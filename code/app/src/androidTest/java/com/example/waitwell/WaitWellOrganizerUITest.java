package com.example.waitwell;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.waitwell.activities.CoOrganizerInviteResponseActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation UI tests for Organizer screens.
 *
 * All organizer activities except CoOrganizerInviteResponseActivity call
 * FirebaseFirestore.getInstance() or FirebaseHelper.getInstance() during
 * onCreate, which triggers a protobuf version conflict at runtime:
 *
 *   NoSuchMethodError: No static method registerDefaultInstance
 *   in com.google.protobuf.GeneratedMessageLite
 *
 * This crashes the activity before any UI inflates, making those screens
 * untestable without either fixing the protobuf dependency tree or mocking
 * Firebase entirely.
 *
 * CoOrganizerInviteResponseActivity is the only screen that renders purely from Intent extras and
 * never touches Firebase during onCreate.
 *
 * Created with the help from Claude (claude.ai)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitWellOrganizerUITest {

    // helpers
    private Intent coOrgIntent(String eventId, String eventName, String message) {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, CoOrganizerInviteResponseActivity.class);
        intent.putExtra(CoOrganizerInviteResponseActivity.EXTRA_EVENT_ID, eventId);
        intent.putExtra(CoOrganizerInviteResponseActivity.EXTRA_EVENT_NAME, eventName);
        if (message != null) {
            intent.putExtra(CoOrganizerInviteResponseActivity.EXTRA_MESSAGE, message);
        }
        return intent;
    }

    private Intent coOrgIntent(String eventId, String eventName) {
        return coOrgIntent(eventId, eventName, null);
    }

    // ── Title rendering ───────────────────────────────────────────

    @Test
    public void coOrgInvite_rendersEventNameFromIntent() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-001", "Jazz Night", "You are invited!"))) {
            onView(withId(R.id.txtCoOrgInviteTitle))
                    .check(matches(withText("Jazz Night")));
        }
    }

    @Test
    public void coOrgInvite_titleView_isDisplayed() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-002", "Pottery Workshop"))) {
            onView(withId(R.id.txtCoOrgInviteTitle)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void coOrgInvite_differentEventName_rendersCorrectly() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-003", "Art Gala", "Please co-host!"))) {
            onView(withId(R.id.txtCoOrgInviteTitle))
                    .check(matches(withText("Art Gala")));
        }
    }

    @Test
    public void coOrgInvite_longEventName_rendersWithoutCrash() {
        String longName = "Annual International Community Music and Dance Festival 2026";
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-004", longName))) {
            onView(withId(R.id.txtCoOrgInviteTitle))
                    .check(matches(withText(longName)));
        }
    }

    @Test
    public void coOrgInvite_specialCharactersInName_rendersCorrectly() {
        String name = "Café & Crêpes — A Night Out!";
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-005", name))) {
            onView(withId(R.id.txtCoOrgInviteTitle))
                    .check(matches(withText(name)));
        }
    }

    @Test
    public void coOrgInvite_singleWordName_rendersCorrectly() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-006", "Hackathon"))) {
            onView(withId(R.id.txtCoOrgInviteTitle))
                    .check(matches(withText("Hackathon")));
        }
    }

    // ── Message rendering ─────────────────────────────────────────

    @Test
    public void coOrgInvite_rendersMessageFromIntent() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-010", "Jazz Night", "You are invited!"))) {
            onView(withId(R.id.txtCoOrgInviteMessage))
                    .check(matches(withText("You are invited!")));
        }
    }

    @Test
    public void coOrgInvite_messageView_isDisplayed() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-011", "Workshop", "Join us!"))) {
            onView(withId(R.id.txtCoOrgInviteMessage)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void coOrgInvite_differentMessage_rendersCorrectly() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(
                             coOrgIntent("evt-012", "Gala", "We need your help organizing!"))) {
            onView(withId(R.id.txtCoOrgInviteMessage))
                    .check(matches(withText("We need your help organizing!")));
        }
    }

    @Test
    public void coOrgInvite_longMessage_rendersWithoutCrash() {
        String msg = "You have been selected as a co-organizer for this very important "
                + "community event. Please accept or decline at your earliest convenience.";
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-013", "Event", msg))) {
            onView(withId(R.id.txtCoOrgInviteMessage))
                    .check(matches(withText(msg)));
        }
    }

    // ── Accept button ─────────────────────────────────────────────

    @Test
    public void coOrgInvite_acceptButton_isDisplayed() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-020", "Book Club"))) {
            onView(withId(R.id.btnAcceptCoOrg)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void coOrgInvite_acceptButton_isEnabled() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-021", "Yoga Session"))) {
            onView(withId(R.id.btnAcceptCoOrg)).check(matches(isEnabled()));
        }
    }

    // ── Decline button ────────────────────────────────────────────

    @Test
    public void coOrgInvite_declineButton_isDisplayed() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-030", "Book Club"))) {
            onView(withId(R.id.btnDeclineCoOrg)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void coOrgInvite_declineButton_isEnabled() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-031", "Yoga Session"))) {
            onView(withId(R.id.btnDeclineCoOrg)).check(matches(isEnabled()));
        }
    }

    // ── Back button ───────────────────────────────────────────────

    @Test
    public void coOrgInvite_backButton_isDisplayed() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-040", "Running Club"))) {
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void coOrgInvite_backButton_isEnabled() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-041", "Running Club"))) {
            onView(withId(R.id.btnBack)).check(matches(isEnabled()));
        }
    }

    // ── Combined checks ───────────────────────────────────────────

    @Test
    public void coOrgInvite_allThreeButtons_areDisplayedTogether() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-050", "Trivia Night"))) {
            onView(withId(R.id.btnAcceptCoOrg)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeclineCoOrg)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void coOrgInvite_allThreeButtons_areEnabledTogether() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(coOrgIntent("evt-051", "Trivia Night"))) {
            onView(withId(R.id.btnAcceptCoOrg)).check(matches(isEnabled()));
            onView(withId(R.id.btnDeclineCoOrg)).check(matches(isEnabled()));
            onView(withId(R.id.btnBack)).check(matches(isEnabled()));
        }
    }

    @Test
    public void coOrgInvite_titleAndMessage_bothRendered() {
        try (ActivityScenario<CoOrganizerInviteResponseActivity> ignored =
                     ActivityScenario.launch(
                             coOrgIntent("evt-052", "Salsa Night", "Come dance with us!"))) {
            onView(withId(R.id.txtCoOrgInviteTitle))
                    .check(matches(withText("Salsa Night")));
            onView(withId(R.id.txtCoOrgInviteMessage))
                    .check(matches(withText("Come dance with us!")));
            onView(withId(R.id.btnAcceptCoOrg)).check(matches(isDisplayed()));
            onView(withId(R.id.btnDeclineCoOrg)).check(matches(isDisplayed()));
        }
    }
}