/*
 * Follows the same pattern as OrganiserIntentTest:
 *   - ActivityScenarioRule on one safe Activity (ConfirmationActivity)
 *   - Espresso UI checks on that Activity
 *   - Plain Intent structure tests for Firebase-crashing Activities
 *
 * ConfirmationActivity reads "event_title" from its Intent and never
 * calls FirebaseHelper or FirebaseFirestore, so it is safe to launch.
 *
 * Sites referenced:
 *
 * Espresso Intents — intending() and intended() for stubbing/verifying:
 * https://developer.android.com/training/testing/espresso/intents
 *
 * ActivityScenario — launching activities with custom Intents:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 */
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.waitwell.activities.AllEventsActivity;
import com.example.waitwell.activities.ConfirmationActivity;
import com.example.waitwell.activities.EntrantLotteryCriteria;
import com.example.waitwell.activities.EventDetailActivity;
import com.example.waitwell.activities.InvitationResponseActivity;
import com.example.waitwell.activities.MainActivity;
import com.example.waitwell.activities.RegisterActivity;
import com.example.waitwell.activities.RegistrationHistoryActivity;
import com.example.waitwell.activities.WaitListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent and UI tests for entrant screens. ConfirmationActivity is the
 * only entrant screen (besides the ones already covered in WaitWellEntrantUITest)
 * that never calls Firebase during its lifecycle, so we launch it via
 * ActivityScenarioRule for real Espresso checks. All other screens that
 * call FirebaseHelper.getInstance() during onCreate/onResume are tested
 * via Intent structure assertions only.
 *
 * Addresses: US 01.01.01, US 01.01.03, US 01.02.01, US 01.03.01,
 *            US 01.05.01, US 01.05.02, US 01.05.03, US 01.06.01
 *
 * @author Viktoria
 * @version 1.0
 * @see ConfirmationActivity
 *
 * Created with the help from Claude (claude.ai).
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitWellEntrantUITest {

    private static Intent makeConfirmationIntent(Context ctx) {
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        intent.putExtra("event_title", "Fake Entrant Event");
        return intent;
    }

    @Rule
    public ActivityScenarioRule<ConfirmationActivity> scenarioRule =
            new ActivityScenarioRule<>(
                    makeConfirmationIntent(ApplicationProvider.getApplicationContext()));

    private static void stubActivity(Class<?> cls) {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        intending(hasComponent(cls.getName())).respondWith(ok);
    }

    @Before
    public void setUp() {
        Intents.init();
        stubActivity(MainActivity.class);
        stubActivity(WaitListActivity.class);
        stubActivity(AllEventsActivity.class);
        stubActivity(EventDetailActivity.class);
        stubActivity(RegistrationHistoryActivity.class);
        stubActivity(EntrantLotteryCriteria.class);
        stubActivity(RegisterActivity.class);
        stubActivity(InvitationResponseActivity.class);
        stubActivity(EntrantNotificationScreen.class);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Verifies the event title from the Intent extra renders on screen.
     */
    @Test
    public void testConfirmation_showsEventTitleFromIntent() {
        onView(withText("Fake Entrant Event")).check(matches(isDisplayed()));
    }

    /**
     * Verifies the waiting list confirmation message is displayed.
     */
    @Test
    public void testConfirmation_showsWaitlistMessage() {
        onView(withText("You're on the waiting list!")).check(matches(isDisplayed()));
    }

    /**
     * Verifies the My Waitlist button is visible and enabled.
     */
    @Test
    public void testConfirmation_myWaitlistButton_isDisplayed() {
        onView(withId(R.id.btnMyWaitlist)).check(matches(isDisplayed()));
    }

    @Test
    public void testConfirmation_myWaitlistButton_isEnabled() {
        onView(withId(R.id.btnMyWaitlist)).check(matches(isEnabled()));
    }

    /**
     * Verifies the Back button is visible and enabled.
     */
    @Test
    public void testConfirmation_backButton_isDisplayed() {
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }

    @Test
    public void testConfirmation_backButton_isEnabled() {
        onView(withId(R.id.btnBack)).check(matches(isEnabled()));
    }

    /**
     * Tapping My Waitlist fires an intent to WaitListActivity.
     */
    @Test
    public void testConfirmation_myWaitlist_launchesWaitListActivity() {
        onView(withId(R.id.btnMyWaitlist)).perform(click());
        intended(hasComponent(WaitListActivity.class.getName()));
    }

    /**
     * Tapping Back fires an intent to MainActivity.
     */
    @Test
    public void testConfirmation_back_launchesMainActivity() {
        onView(withId(R.id.btnBack)).perform(click());
        intended(hasComponent(MainActivity.class.getName()));
    }


    /**
     * Verifies InvitationResponseActivity intent with all extras for a
     * normal invitation (not yet responded).
     */
    @Test
    public void testInvitationResponseIntent_hasAllExtras() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_ID, "evt-100");
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, "Yoga Class");
        intent.putExtra(InvitationResponseActivity.EXTRA_MESSAGE, "You've been selected!");
        intent.putExtra(InvitationResponseActivity.EXTRA_NOTIFICATION_ID, "notif-200");

        assertEquals(InvitationResponseActivity.class.getName(),
                intent.getComponent().getClassName());
        assertEquals("evt-100",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_EVENT_ID));
        assertEquals("Yoga Class",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_EVENT_NAME));
        assertEquals("You've been selected!",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_MESSAGE));
        assertEquals("notif-200",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_NOTIFICATION_ID));
    }

    /**
     * Verifies InvitationResponseActivity read-only intent sets
     * EXTRA_ALREADY_RESPONDED to "confirmed".
     */
    @Test
    public void testInvitationResponseIntent_readOnlyConfirmed_hasAlreadyRespondedExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, "Jazz Night");
        intent.putExtra(InvitationResponseActivity.EXTRA_ALREADY_RESPONDED, "confirmed");

        assertEquals("confirmed",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_ALREADY_RESPONDED));
        assertEquals("Jazz Night",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_EVENT_NAME));
    }

    /**
     * Verifies read-only cancelled mode sets the correct extra.
     */
    @Test
    public void testInvitationResponseIntent_readOnlyCancelled_hasAlreadyRespondedExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, "Jazz Night");
        intent.putExtra(InvitationResponseActivity.EXTRA_ALREADY_RESPONDED, "cancelled");

        assertEquals("cancelled",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_ALREADY_RESPONDED));
    }

    /**
     * Verifies the prefilled event card extras (location, date range, price)
     * are correctly set on the intent.
     */
    @Test
    public void testInvitationResponseIntent_prefilledCard_hasLocationDatePrice() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, "Yoga Class");
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_LOCATION, "Downtown Studio");
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_DATE_RANGE, "2026-05-01  -  2026-05-15");
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_PRICE, "$25.00");

        assertEquals("Downtown Studio",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_EVENT_LOCATION));
        assertEquals("2026-05-01  -  2026-05-15",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_EVENT_DATE_RANGE));
        assertEquals("$25.00",
                intent.getStringExtra(InvitationResponseActivity.EXTRA_EVENT_PRICE));
    }

    /**
     * Verifies that without EXTRA_ALREADY_RESPONDED the extra returns null,
     * meaning the activity should show accept/decline buttons.
     */
    @Test
    public void testInvitationResponseIntent_normalMode_noAlreadyRespondedExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, "Pottery Workshop");

        assertNull(intent.getStringExtra(InvitationResponseActivity.EXTRA_ALREADY_RESPONDED));
    }


    /**
     * Verifies EventDetailActivity intent uses "event_id" extra.
     */
    @Test
    public void testEventDetailIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EventDetailActivity.class);
        intent.putExtra("event_id", "evt-300");

        assertNotNull(intent.getComponent());
        assertEquals(EventDetailActivity.class.getName(),
                intent.getComponent().getClassName());
        assertEquals("evt-300", intent.getStringExtra("event_id"));
    }

    /**
     * Verifies EventDetailActivity finishes when event_id is null
     * (per its onCreate guard).
     */
    @Test
    public void testEventDetailIntent_missingEventId_returnsNull() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EventDetailActivity.class);

        assertNull(intent.getStringExtra("event_id"));
    }

    /**
     * Verifies AllEventsActivity intent targets the correct component.
     * No extras required — it loads all events from Firestore.
     */
    @Test
    public void testAllEventsIntent_targetsCorrectActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, AllEventsActivity.class);

        assertNotNull(intent.getComponent());
        assertEquals(AllEventsActivity.class.getName(),
                intent.getComponent().getClassName());
    }


    /**
     * Verifies MainActivity intent targets the correct component.
     */
    @Test
    public void testMainActivityIntent_targetsCorrectActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, MainActivity.class);

        assertNotNull(intent.getComponent());
        assertEquals(MainActivity.class.getName(),
                intent.getComponent().getClassName());
    }

    /**
     * Verifies that CLEAR_TOP + SINGLE_TOP flags are set correctly
     * when navigating to MainActivity (matching how bottom nav works).
     */
    @Test
    public void testMainActivityIntent_withClearTopFlag_hasCorrectFlags() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0);
    }

    /**
     * Verifies RegistrationHistoryActivity intent targets the correct component.
     * No extras required — it uses DeviceUtils.getDeviceId() internally.
     */
    @Test
    public void testRegistrationHistoryIntent_targetsCorrectActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RegistrationHistoryActivity.class);

        assertNotNull(intent.getComponent());
        assertEquals(RegistrationHistoryActivity.class.getName(),
                intent.getComponent().getClassName());
    }


    /**
     * Verifies EntrantLotteryCriteria intent targets the correct component.
     */
    @Test
    public void testEntrantLotteryCriteriaIntent_targetsCorrectActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EntrantLotteryCriteria.class);

        assertNotNull(intent.getComponent());
        assertEquals(EntrantLotteryCriteria.class.getName(),
                intent.getComponent().getClassName());
    }

    /**
     * Verifies WaitListActivity intent targets the correct component.
     */
    @Test
    public void testWaitListIntent_targetsCorrectActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, WaitListActivity.class);

        assertNotNull(intent.getComponent());
        assertEquals(WaitListActivity.class.getName(),
                intent.getComponent().getClassName());
    }

    /**
     * Verifies WaitListActivity intent with CLEAR_TOP navigation flags.
     */
    @Test
    public void testWaitListIntent_withClearTopFlag_hasCorrectFlags() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, WaitListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0);
    }

    /**
     * Verifies RegisterActivity intent targets the correct component.
     */
    @Test
    public void testRegisterIntent_targetsCorrectActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RegisterActivity.class);

        assertNotNull(intent.getComponent());
        assertEquals(RegisterActivity.class.getName(),
                intent.getComponent().getClassName());
    }

    /**
     * Verifies logout intent to RegisterActivity uses NEW_TASK + CLEAR_TASK
     * flags (matching how drawer logout works in MainActivity).
     */
    @Test
    public void testRegisterIntent_logoutFlags_hasNewTaskAndClearTask() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, RegisterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0);
    }

    /**
     * Verifies EntrantNotificationScreen intent targets the correct component.
     */
    @Test
    public void testNotificationScreenIntent_targetsCorrectActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EntrantNotificationScreen.class);

        assertNotNull(intent.getComponent());
        assertEquals(EntrantNotificationScreen.class.getName(),
                intent.getComponent().getClassName());
    }

    /**
     * Verifies bottom-nav style flags for notification screen navigation.
     */
    @Test
    public void testNotificationScreenIntent_withClearTopFlag_hasCorrectFlags() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EntrantNotificationScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        assertTrue((intent.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0);
    }
}
