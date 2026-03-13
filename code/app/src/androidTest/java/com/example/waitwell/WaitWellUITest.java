package com.example.waitwell;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.waitwell.activities.AdminEventRemovedActivity;
import com.example.waitwell.activities.AdminEventsActivity;
import com.example.waitwell.activities.AdminMainMenuActivity;
import com.example.waitwell.activities.AdminProfileRemovedActivity;
import com.example.waitwell.activities.AdminProfilesActivity;
import com.example.waitwell.activities.AllEventsActivity;
import com.example.waitwell.activities.ConfirmationActivity;
import com.example.waitwell.activities.EventDetailActivity;
import com.example.waitwell.activities.MainActivity;
import com.example.waitwell.activities.OrganizerEntryActivity;
import com.example.waitwell.activities.WaitListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Lab 7 style Espresso tests: every button is clickable and does not crash.
 * No Firestore logic CAUSE it kept crashing and won't work
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitWellUITest {

    @Rule
    public ActivityScenarioRule<ConfirmationActivity> scenario =
            new ActivityScenarioRule<>(ConfirmationActivity.class);

    @Before
    public void initIntents() {
        androidx.test.espresso.intent.Intents.init();
        intending(hasComponent(AllEventsActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(EventDetailActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(WaitListActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(MainActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(AdminEventsActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(AdminProfilesActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(AdminEventRemovedActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(AdminProfileRemovedActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    @After
    public void releaseIntents() {
        androidx.test.espresso.intent.Intents.release();
    }

    // ----- Entrant Button Tests -----

    @Test
    public void testEntrant_btnViewAll_clickable() {
        // open main then tap view all – should try to open events list
        androidx.test.core.app.ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.btnViewAll)).perform(click());
        intended(hasComponent(AllEventsActivity.class.getName()));
    }

    @Test
    public void testEntrant_btnJoinWaitlist_clickable() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EventDetailActivity.class);
        intent.putExtra("event_id", "test-event-id");
        ActivityScenario.launch(intent);
        onView(withId(R.id.btnJoinWaitlist)).perform(click());
        intended(hasComponent(ConfirmationActivity.class.getName()));
    }

    @Test
    public void testEntrant_btnMyWaitlist_clickable() {
        // from confirmation screen go to my waitlist
        onView(withId(R.id.btnMyWaitlist)).perform(click());
        intended(hasComponent(WaitListActivity.class.getName()));
    }

    @Test
    public void testEntrant_btnQuit_clickable() {
        androidx.test.core.app.ActivityScenario.launch(WaitListActivity.class);
        onView(withId(R.id.btnQuit)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.scrollEntries)).check(matches(isDisplayed()));
    }

    @Test
    public void testEntrant_btnBack_clickable() {
        onView(withId(R.id.btnBack)).perform(click());
        intended(hasComponent(MainActivity.class.getName()));
    }

    // ----- Organizer Button Tests -----
    // From OrganizerEntryActivity. Create flow does not need list data.

    @Test
    public void testOrganizer_btnCreateNewEvent_clickable() {
        // open organizer home and tap create new event
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.btnCreateNewEvent)).perform(click());
        onView(withId(R.id.editEventName)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizer_btnSubmitEvent_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.btnCreateNewEvent)).perform(click());
        onView(withId(R.id.editEventName)).perform(typeText("Test Event"), closeSoftKeyboard());
        onView(withId(R.id.editLocation)).perform(typeText("Here"), closeSoftKeyboard());
        onView(withId(R.id.btnSubmitEvent)).perform(click());
        onView(withId(R.id.btnViewMyEvents)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizer_btnViewMyEvents_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.btnCreateNewEvent)).perform(click());
        onView(withId(R.id.editEventName)).perform(typeText("E"), closeSoftKeyboard());
        onView(withId(R.id.editLocation)).perform(typeText("L"), closeSoftKeyboard());
        onView(withId(R.id.btnSubmitEvent)).perform(click());
        onView(withId(R.id.btnViewMyEvents)).perform(click());
        onView(withId(R.id.btnCreateNewEvent)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizer_item_organizer_btn_manage_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.organizer_events_list)).perform(scrollTo());
        onView(withId(R.id.item_organizer_btn_manage)).perform(click());
        onView(withId(R.id.btnEditEvent)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizer_btnEditEvent_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.organizer_events_list)).perform(scrollTo());
        onView(withId(R.id.item_organizer_btn_manage)).perform(click());
        onView(withId(R.id.btnEditEvent)).perform(click());
        onView(withId(R.id.editEventName)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizer_btnViewCanceledEntrants_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.organizer_events_list)).perform(scrollTo());
        onView(withId(R.id.item_organizer_btn_manage)).perform(click());
        onView(withId(R.id.btnViewCanceledEntrants)).perform(click());
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizer_btnViewInvitedEntrants_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.organizer_events_list)).perform(scrollTo());
        onView(withId(R.id.item_organizer_btn_manage)).perform(click());
        onView(withId(R.id.btnViewInvitedEntrants)).perform(click());
        onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
    }

    @Test
    public void testOrganizer_btnDrawReplacement_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.organizer_events_list)).perform(scrollTo());
        onView(withId(R.id.item_organizer_btn_manage)).perform(click());
        onView(withId(R.id.btnDrawReplacement)).check(matches(isDisplayed())).perform(click());
    }

    @Test
    public void testOrganizer_btnBack_clickable() {
        androidx.test.core.app.ActivityScenario.launch(OrganizerEntryActivity.class);
        onView(withId(R.id.organizer_events_list)).perform(scrollTo());
        onView(withId(R.id.item_organizer_btn_manage)).perform(click());
        onView(withId(R.id.btnOrganizerBack)).check(matches(isDisplayed())).perform(click());
        onView(withId(R.id.btnCreateNewEvent)).check(matches(isDisplayed()));
    }

    // ----- Admin Button Tests -----

    @Test
    public void testAdmin_btnAllEvents_clickable() {
        // admin menu -> all events
        androidx.test.core.app.ActivityScenario.launch(AdminMainMenuActivity.class);
        onView(withId(R.id.btnAllEvents)).perform(click());
        intended(hasComponent(AdminEventsActivity.class.getName()));
    }

    @Test
    public void testAdmin_btnAllProfiles_clickable() {
        androidx.test.core.app.ActivityScenario.launch(AdminMainMenuActivity.class);
        onView(withId(R.id.btnAllProfiles)).perform(click());
        intended(hasComponent(AdminProfilesActivity.class.getName()));
    }

    @Test
    public void testAdmin_btnRemoveEvent_clickable() {
        androidx.test.core.app.ActivityScenario.launch(AdminEventsActivity.class);
        onView(withId(R.id.recyclerEvents)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRemoveEvent)).perform(click());
        intended(hasComponent(AdminEventRemovedActivity.class.getName()));
    }

    @Test
    public void testAdmin_btnRemoveProfile_clickable() {
        androidx.test.core.app.ActivityScenario.launch(AdminProfilesActivity.class);
        onView(withId(R.id.recyclerProfiles)).check(matches(isDisplayed()));
        onView(withId(R.id.btnRemoveProfile)).perform(click());
        intended(hasComponent(AdminProfileRemovedActivity.class.getName()));
    }

    @Test
    public void testAdmin_backBtn_clickable() {
        androidx.test.core.app.ActivityScenario.launch(AdminEventRemovedActivity.class);
        onView(withId(R.id.backBtn)).perform(click());
        // just confirm we didn't crash – activity finishes
    }

    @Test
    public void testAdmin_backBtn_profilesRemoved_clickable() {
        androidx.test.core.app.ActivityScenario.launch(AdminProfileRemovedActivity.class);
        onView(withId(R.id.backBtn)).check(matches(isDisplayed())).perform(click());
    }
}
