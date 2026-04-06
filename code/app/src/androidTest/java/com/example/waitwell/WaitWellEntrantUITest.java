package com.example.waitwell;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.waitwell.activities.EntrantLotteryCriteria;
import com.example.waitwell.activities.InvitationResponseActivity;
import com.example.waitwell.activities.MainActivity;
import com.example.waitwell.activities.WaitListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *  Instrumentation UI tests for Entrant screens.
 *
 * Due to a protobuf version conflict (NoSuchMethodError on
 * GeneratedMessageLite.registerDefaultInstance), any activity that calls
 * FirebaseHelper.getInstance() or FirebaseFirestore.getInstance() during
 * its lifecycle will crash before the UI inflates.
 *
 *Tested other screens that do not raise this issue
 *
 * Created with the help from Claude (claude.ai)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitWellEntrantUITest {

    @Before
    public void setUp() {
        Intents.init();

        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);

        intending(hasComponent(MainActivity.class.getName())).respondWith(ok);
        intending(hasComponent(WaitListActivity.class.getName())).respondWith(ok);
        intending(hasComponent(EntrantNotificationScreen.class.getName())).respondWith(ok);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    // ── helper ─────────────────────────────────────────────────────

    /**
     * Builds an InvitationResponseActivity intent that bypasses Firebase.
     * Key: do NOT pass EXTRA_EVENT_ID — this skips loadEventFromFirestore().
     */
    private Intent invitationIntent(String eventName, String alreadyResponded) {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, eventName);
        if (alreadyResponded != null) {
            intent.putExtra(InvitationResponseActivity.EXTRA_ALREADY_RESPONDED, alreadyResponded);
        }
        return intent;
    }

    private Intent invitationIntentWithCard(String eventName, String location,
                                            String dateRange, String price) {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, eventName);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_LOCATION, location);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_DATE_RANGE, dateRange);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_PRICE, price);
        return intent;
    }


    // EntrantLotteryCriteria — zero Firebase, fully static UI

    @Test
    public void lotteryCriteria_hamburgerButton_isDisplayed() {
        try (ActivityScenario<EntrantLotteryCriteria> ignored =
                     ActivityScenario.launch(EntrantLotteryCriteria.class)) {
            onView(withId(R.id.btnHamburger)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void lotteryCriteria_drawerLayout_isDisplayed() {
        try (ActivityScenario<EntrantLotteryCriteria> ignored =
                     ActivityScenario.launch(EntrantLotteryCriteria.class)) {
            onView(withId(R.id.drawer_layout)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void lotteryCriteria_bottomNavigation_isDisplayed() {
        try (ActivityScenario<EntrantLotteryCriteria> ignored =
                     ActivityScenario.launch(EntrantLotteryCriteria.class)) {
            onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void lotteryCriteria_hamburgerButton_isEnabled() {
        try (ActivityScenario<EntrantLotteryCriteria> ignored =
                     ActivityScenario.launch(EntrantLotteryCriteria.class)) {
            onView(withId(R.id.btnHamburger)).check(matches(isEnabled()));
        }
    }

    @Test
    public void lotteryCriteria_hamburgerButton_isClickable() {
        try (ActivityScenario<EntrantLotteryCriteria> ignored =
                     ActivityScenario.launch(EntrantLotteryCriteria.class)) {
            onView(withId(R.id.btnHamburger)).perform(click());
        }
    }

    // ════════════════════════════════════════════════════════════════
    // InvitationResponseActivity — read-only mode (already responded)
    // No EXTRA_EVENT_ID → loadEventFromFirestore() is never called
    // ════════════════════════════════════════════════════════════════

    @Test
    public void invitationResponse_readOnly_confirmed_showsAcceptedText() {
        Context ctx = ApplicationProvider.getApplicationContext();
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.message))
                    .check(matches(withText(
                            ctx.getString(R.string.toast_invitation_already_accepted))));
        }
    }

    @Test
    public void invitationResponse_readOnly_cancelled_showsDeclinedText() {
        Context ctx = ApplicationProvider.getApplicationContext();
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "cancelled"))) {
            onView(withId(R.id.message))
                    .check(matches(withText(
                            ctx.getString(R.string.toast_invitation_already_declined))));
        }
    }

    @Test
    public void invitationResponse_readOnly_hidesAcceptAndDeclineButtons() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.accept)).check(matches(not(isDisplayed())));
            onView(withId(R.id.decline)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void invitationResponse_readOnly_showsBackButton() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.back_button)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invitationResponse_readOnly_backButton_isEnabled() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.back_button)).check(matches(isEnabled()));
        }
    }

    @Test
    public void invitationResponse_readOnly_backButton_launchesNotificationScreen() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.back_button)).perform(click());
            intended(hasComponent(EntrantNotificationScreen.class.getName()));
        }
    }

    @Test
    public void invitationResponse_readOnly_hamburgerButton_isDisplayed() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.btnHamburger)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invitationResponse_readOnly_profileAvatar_isDisplayed() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.imgProfileAvatar)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invitationResponse_readOnly_eventTitle_showsName() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", "confirmed"))) {
            onView(withId(R.id.eventTitle))
                    .check(matches(withText("Pottery Workshop")));
        }
    }

    @Test
    public void invitationResponse_readOnly_messageView_isDisplayed() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Jazz Night", "confirmed"))) {
            onView(withId(R.id.message)).check(matches(isDisplayed()));
        }
    }

    // ── InvitationResponseActivity — normal mode (not yet responded) ──

    @Test
    public void invitationResponse_normalMode_showsAcceptAndDeclineButtons() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.accept)).check(matches(isDisplayed()));
            onView(withId(R.id.decline)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invitationResponse_normalMode_acceptButton_isEnabled() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.accept)).check(matches(isEnabled()));
        }
    }

    @Test
    public void invitationResponse_normalMode_declineButton_isEnabled() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.decline)).check(matches(isEnabled()));
        }
    }

    @Test
    public void invitationResponse_normalMode_rendersEventTitle() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.eventTitle))
                    .check(matches(withText("Pottery Workshop")));
        }
    }

    @Test
    public void invitationResponse_normalMode_backButton_launchesNotificationScreen() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.back_button)).perform(click());
            intended(hasComponent(EntrantNotificationScreen.class.getName()));
        }
    }

    @Test
    public void invitationResponse_normalMode_backButton_isDisplayed() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.back_button)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invitationResponse_normalMode_hamburgerButton_isDisplayed() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.btnHamburger)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void invitationResponse_normalMode_profileAvatar_isDisplayed() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Pottery Workshop", null))) {
            onView(withId(R.id.imgProfileAvatar)).check(matches(isDisplayed()));
        }
    }

    // ── InvitationResponseActivity — prefilled event card from extras ──

    @Test
    public void invitationResponse_prefilledCard_showsLocation() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntentWithCard(
                             "Yoga Class", "Downtown Studio",
                             "2026-05-01  -  2026-05-15", "$25.00"))) {
            onView(withId(R.id.txtEventLocation))
                    .check(matches(withText("Downtown Studio")));
        }
    }

    @Test
    public void invitationResponse_prefilledCard_showsDateRange() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntentWithCard(
                             "Yoga Class", "Downtown Studio",
                             "2026-05-01  -  2026-05-15", "$25.00"))) {
            onView(withId(R.id.txtEventDateRange))
                    .check(matches(withText("2026-05-01  -  2026-05-15")));
        }
    }

    @Test
    public void invitationResponse_prefilledCard_showsPrice() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntentWithCard(
                             "Yoga Class", "Downtown Studio",
                             "2026-05-01  -  2026-05-15", "$25.00"))) {
            onView(withId(R.id.txtEventPrice))
                    .check(matches(withText("$25.00")));
        }
    }

    @Test
    public void invitationResponse_prefilledCard_showsFreePrice() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntentWithCard(
                             "Free Meetup", "City Park",
                             "2026-06-01  -  2026-06-01", "Free"))) {
            onView(withId(R.id.txtEventPrice))
                    .check(matches(withText("Free")));
        }
    }

    @Test
    public void invitationResponse_prefilledCard_showsEventTitle() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntentWithCard(
                             "Yoga Class", "Downtown Studio",
                             "2026-05-01  -  2026-05-15", "$25.00"))) {
            onView(withId(R.id.eventTitle))
                    .check(matches(withText("Yoga Class")));
        }
    }

    @Test
    public void invitationResponse_differentEventName_rendersCorrectly() {
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent("Art Gala", "confirmed"))) {
            onView(withId(R.id.eventTitle))
                    .check(matches(withText("Art Gala")));
        }
    }

    @Test
    public void invitationResponse_longEventName_rendersWithoutCrash() {
        String longName = "Annual International Community Music and Dance Festival 2026";
        try (ActivityScenario<InvitationResponseActivity> ignored =
                     ActivityScenario.launch(invitationIntent(longName, "confirmed"))) {
            onView(withId(R.id.eventTitle))
                    .check(matches(withText(longName)));
        }
    }
}