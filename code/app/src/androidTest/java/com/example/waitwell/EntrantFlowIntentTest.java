package com.example.waitwell;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.waitwell.activities.AllEventsActivity;
import com.example.waitwell.activities.ConfirmationActivity;
import com.example.waitwell.activities.EventDetailActivity;
import com.example.waitwell.activities.MainActivity;
import com.example.waitwell.activities.WaitListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented intent tests for entrant flow screens.
 * Runs on emulator/device. Tests actual navigation between activities.
 *
 * Required dependency:
 *   androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
 */
@RunWith(AndroidJUnit4.class)
public class EntrantFlowIntentTest {

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    //MainActivity

    @Test
    public void mainActivity_rendersKeyElements() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.searchBar)).check(matches(isDisplayed()));
            onView(withId(R.id.btnScanQr)).check(matches(isDisplayed()));
            onView(withId(R.id.btnViewAll)).check(matches(isDisplayed()));
            onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void mainActivity_viewAll_launchesAllEvents() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.btnViewAll)).perform(click());
            intended(hasComponent(AllEventsActivity.class.getName()));
        }
    }

    // AllEventsActivity

    @Test
    public void allEvents_rendersSearchAndChips() {
        try (ActivityScenario<AllEventsActivity> ignored = ActivityScenario.launch(AllEventsActivity.class)) {
            onView(withId(R.id.editSearch)).check(matches(isDisplayed()));
            onView(withId(R.id.chipAll)).check(matches(isDisplayed()));
            onView(withId(R.id.chipOpen)).check(matches(isDisplayed()));
            onView(withId(R.id.chipCategory)).check(matches(isDisplayed()));
        }
    }

    // EventDetailActivity



    @Test
    public void eventDetail_backFinishes() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, EventDetailActivity.class);
        intent.putExtra("event_id", "test_id");

        try (ActivityScenario<EventDetailActivity> scenario = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnBack)).perform(click());

            scenario.onActivity(activity -> {
                assertTrue(activity.isFinishing() || activity.isDestroyed());
            });
        }
    }


    // ConfirmationActivity

    @Test
    public void confirmation_showsEventTitle() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        intent.putExtra("event_title", "Yoga Hatha");

        try (ActivityScenario<ConfirmationActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withText("Yoga Hatha")).check(matches(isDisplayed()));
            onView(withText("You're on the waiting list!")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void confirmation_defaultTitleWhenMissing() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        // no event_title extra

        try (ActivityScenario<ConfirmationActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withText("Event")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void confirmation_myWaitlistOpensWaitList() {
        Context ctx = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(ctx, ConfirmationActivity.class);
        intent.putExtra("event_title", "Swimming Lessons");

        try (ActivityScenario<ConfirmationActivity> ignored = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnMyWaitlist)).perform(click());
            intended(hasComponent(WaitListActivity.class.getName()));
        }
    }

    // WaitListActivity
    @Test
    public void waitList_rendersCorrectly() {
        try (ActivityScenario<WaitListActivity> ignored = ActivityScenario.launch(WaitListActivity.class)) {
            onView(withText("Your WaitList")).check(matches(isDisplayed()));
            onView(withId(R.id.bottomNavigation)).check(matches(isDisplayed()));
        }
    }
}
