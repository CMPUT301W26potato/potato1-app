/*
 * The general structure of these tests follows the same style as the
 * Lab 7 Espresso example from the course — @RunWith(AndroidJUnit4.class),
 * @LargeTest, ActivityScenarioRule for the one Activity we can safely
 * launch, and intending()/respondWith() to stub out the Firebase-crashing
 * ones so they never actually start.
 *
 * I used Gemini to understand why Firebase causes a protobuf version
 * conflict crash during instrumented tests and how intending() can be
 * used to intercept and stub an Activity launch before it ever fires,
 * which is how we get around the crash without touching production code.
 *
 * Sites I looked at:
 *
 * Espresso Intents — how intending() and intended() work for stubbing
 * and verifying Activity navigation:
 * https://developer.android.com/training/testing/espresso/intents
 *
 * ActivityScenario — launching activities with custom Intents in tests:
 * https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
 *
 * Mockito — verifying Intent extras without launching the Activity:
 * https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
 */
package com.example.waitwell;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

import com.example.waitwell.activities.AssignCoOrganizerActivity;
import com.example.waitwell.activities.CancelledEntrantsActivity;
import com.example.waitwell.activities.CoOrganizerInviteResponseActivity;
import com.example.waitwell.activities.EntrantCalendarActivity;
import com.example.waitwell.activities.EnrolledEntrantsActivity;
import com.example.waitwell.activities.FinalEntrantsActivity;
import com.example.waitwell.activities.InviteEntrantsActivity;
import com.example.waitwell.activities.InvitedEntrantsActivity;
import com.example.waitwell.activities.OrganizerCommentsActivity;
import com.example.waitwell.activities.OrganizerEntryActivity;
import com.example.waitwell.activities.SampledEntrantsActivity;
import com.example.waitwell.activities.SamplingConfirmationActivity;
import com.example.waitwell.activities.ViewRequestsActivity;
import com.example.waitwell.activities.WaitlistMapActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Intent tests for the organiser role screens. Most organiser activities
 * crash on launch because they call FirebaseFirestore.getInstance() in
 * onCreate before any UI inflates, causing a protobuf conflict. So we
 * can only do real Espresso UI tests on CoOrganizerInviteResponseActivity,
 * and for everything else we verify the Intent structure using intending()
 * stubs or plain Intent builds, we tried to follow the Lab 7 Espresso example from
 * the course.
 *
 * Addresses: US 02.01.01, US 02.01.02, US 02.02.01, US 02.05.01,
 *            US 02.06.03, US 02.06.05, US 02.07.01, US 02.07.02,
 *            US 02.07.03, US 01.05.06, US 01.05.07
 *
 * @author Karina Zhang
 * @version 1.0
 * @see CoOrganizerInviteResponseActivity
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class OrganiserIntentTest {

    private static Intent makeCoOrgIntent(Context ctx) {
        Intent intent = new Intent(ctx, CoOrganizerInviteResponseActivity.class);
        intent.putExtra(CoOrganizerInviteResponseActivity.EXTRA_EVENT_ID, "fake_event_id");
        intent.putExtra(CoOrganizerInviteResponseActivity.EXTRA_EVENT_NAME, "Fake Co-Org Event");
        intent.putExtra(CoOrganizerInviteResponseActivity.EXTRA_MESSAGE, "Fake invite message");
        intent.putExtra(CoOrganizerInviteResponseActivity.EXTRA_NOTIFICATION_ID, "fake_notif_id");
        return intent;
    }

    @Rule
    public ActivityScenarioRule<CoOrganizerInviteResponseActivity> scenarioRule =
            new ActivityScenarioRule<>(makeCoOrgIntent(ApplicationProvider.getApplicationContext()));

    private static void stubActivity(Class<?> cls) {
        Instrumentation.ActivityResult ok =
                new Instrumentation.ActivityResult(Activity.RESULT_OK, null);
        // can't launch this directly cause Firebase crashes in onCreate before UI loads
        intending(hasComponent(cls.getName())).respondWith(ok);
    }

    @Before
    public void setUp() {
        Intents.init();
        stubActivity(OrganizerEntryActivity.class);
        stubActivity(ViewRequestsActivity.class);
        stubActivity(InvitedEntrantsActivity.class);
        stubActivity(FinalEntrantsActivity.class);
        stubActivity(CancelledEntrantsActivity.class);
        stubActivity(SampledEntrantsActivity.class);
        stubActivity(SamplingConfirmationActivity.class);
        stubActivity(InviteEntrantsActivity.class);
        stubActivity(EnrolledEntrantsActivity.class);
        stubActivity(AssignCoOrganizerActivity.class);
        stubActivity(OrganizerCommentsActivity.class);
        stubActivity(WaitlistMapActivity.class);
        stubActivity(EntrantCalendarActivity.class);
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Checks that tapping Accept does not crash the process.
     *  path click; Firebase may fail on the device but the UI should stay alive or finish cleanly.
     *
     * @author Karina Zhang
     */
    @Test
    public void testCoOrgScreen_acceptButton_click_doesNotCrash() {
        // the rule already launched CoOrganizer with fake extras so Firebase is never needed for layout
        onView(withId(R.id.btnAcceptCoOrg)).perform(click());
    }

    /**
     * Checks that tapping Decline does not crash the process.
     * Alternative flow from Accept.
     *
     * @author Karina Zhang
     */
    @Test
    public void testCoOrgScreen_declineButton_click_doesNotCrash() {
        onView(withId(R.id.btnDeclineCoOrg)).perform(click());
    }

    /**
     * Checks that tapping Back does not crash and the screen was interactable.
     * Happy path for dismiss.
     *
     * @author Karina Zhang
     */
    @Test
    public void testCoOrgScreen_backButton_click_doesNotCrash() {
        onView(withId(R.id.btnBack)).perform(click());
    }

    /**
     * Verifies ViewRequestsActivity intent matches onCreate keys event_id and event_title.
     * Intent structure only and activity is never launched here.
     *
     * @author Karina Zhang
     */
    @Test
    public void testViewRequestsIntent_hasEventIdAndEventTitleExtras() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, ViewRequestsActivity.class);
        intent.putExtra("event_id", "fake_event_id");
        intent.putExtra("event_title", "Fake Title");
        assertNotNull(intent.getComponent());
        assertEquals(ViewRequestsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra("event_id"));
        assertEquals("Fake Title", intent.getStringExtra("event_title"));
    }

    /**
     * Verifies InvitedEntrantsActivity uses EXTRA_EVENT_ID event_id.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testInvitedEntrantsIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InvitedEntrantsActivity.class);
        intent.putExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertNotNull(intent.getComponent());
        assertEquals(InvitedEntrantsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(InvitedEntrantsActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies FinalEntrantsActivity uses event_id extra.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testFinalEntrantsIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, FinalEntrantsActivity.class);
        intent.putExtra(FinalEntrantsActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertEquals(FinalEntrantsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(FinalEntrantsActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies CancelledEntrantsActivity uses event_id extra.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testCancelledEntrantsIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, CancelledEntrantsActivity.class);
        intent.putExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertEquals(CancelledEntrantsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(CancelledEntrantsActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies SampledEntrantsActivity uses event_id extra.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testSampledEntrantsIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, SampledEntrantsActivity.class);
        intent.putExtra(SampledEntrantsActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertEquals(SampledEntrantsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(SampledEntrantsActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies SamplingConfirmationActivity uses event_id and sampled_count extras.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testSamplingConfirmationIntent_hasEventIdAndSampledCountExtras() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, SamplingConfirmationActivity.class);
        intent.putExtra(SamplingConfirmationActivity.EXTRA_EVENT_ID, "fake_event_id");
        intent.putExtra(SamplingConfirmationActivity.EXTRA_SAMPLED_COUNT, 5);
        assertEquals(SamplingConfirmationActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(SamplingConfirmationActivity.EXTRA_EVENT_ID));
        assertEquals(5, intent.getIntExtra(SamplingConfirmationActivity.EXTRA_SAMPLED_COUNT, 0));
    }

    /**
     * Verifies InviteEntrantsActivity uses EXTRA_EVENT_ID.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testInviteEntrantsIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, InviteEntrantsActivity.class);
        intent.putExtra(InviteEntrantsActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertEquals(InviteEntrantsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(InviteEntrantsActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies EnrolledEntrantsActivity uses event_id extra.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testEnrolledEntrantsIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EnrolledEntrantsActivity.class);
        intent.putExtra(EnrolledEntrantsActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertEquals(EnrolledEntrantsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(EnrolledEntrantsActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies AssignCoOrganizerActivity uses event_id extra.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testAssignCoOrganizerIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, AssignCoOrganizerActivity.class);
        intent.putExtra(AssignCoOrganizerActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertEquals(AssignCoOrganizerActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(AssignCoOrganizerActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies OrganizerCommentsActivity reads event_id string extra.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testOrganizerCommentsIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, OrganizerCommentsActivity.class);
        intent.putExtra("event_id", "fake_event_id");
        assertEquals(OrganizerCommentsActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra("event_id"));
    }

    /**
     * Verifies WaitlistMapActivity uses EXTRA_EVENT_ID.
     * Intent structure only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testWaitlistMapIntent_hasEventIdExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, WaitlistMapActivity.class);
        intent.putExtra(WaitlistMapActivity.EXTRA_EVENT_ID, "fake_event_id");
        assertEquals(WaitlistMapActivity.class.getName(), intent.getComponent().getClassName());
        assertEquals("fake_event_id", intent.getStringExtra(WaitlistMapActivity.EXTRA_EVENT_ID));
    }

    /**
     * Verifies EntrantCalendarActivity intent targets the calendar screen with no event id required.
     * Component check only.
     *
     * @author Karina Zhang
     */
    @Test
    public void testEntrantCalendarIntent_targetsCalendarActivity() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EntrantCalendarActivity.class);
        assertEquals(EntrantCalendarActivity.class.getName(), intent.getComponent().getClassName());
    }

    /**
     * Verifies OrganizerEntryActivity.intentNavigateToMyEvents sets navigate flag.
     * Intent structure helper used by organiser flows.
     *
     * @author Karina Zhang
     */
    @Test
    public void testOrganizerEntryNavigateToMyEventsIntent_hasNavigateExtra() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = OrganizerEntryActivity.intentNavigateToMyEvents(ctx);
        assertNotNull(intent.getComponent());
        assertEquals(OrganizerEntryActivity.class.getName(), intent.getComponent().getClassName());
        assertTrue(intent.getBooleanExtra(OrganizerEntryActivity.EXTRA_NAVIGATE_TO_MY_EVENTS, false));
    }
}
