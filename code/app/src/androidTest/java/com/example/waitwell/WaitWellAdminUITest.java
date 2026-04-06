package com.example.waitwell;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.waitwell.activities.AdminEventsActivity;
import com.example.waitwell.activities.AdminImagesActivity;
import com.example.waitwell.activities.AdminMainMenuActivity;
import com.example.waitwell.activities.AdminNotificationsActivity;
import com.example.waitwell.activities.AdminProfileRemovedActivity;
import com.example.waitwell.activities.AdminProfilesActivity;
import com.example.waitwell.activities.AdminEventRemovedActivity;
import com.example.waitwell.activities.AdminImageRemovedActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented UI tests for the Admin flow.
 * Verifies that the AdminMainMenuActivity displays all four section buttons
 * (Profiles, Events, Images, Notifications) and that tapping each one launches
 * the correct admin activity. Also checks that the event-removed, profile-removed,
 * and image-removed confirmation screens show the expected banner and back button.
 * Uses Espresso Intents to stub outgoing activities so tests run without Firestore.
 *
 * Created with the help from Claude (claude.ai)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class WaitWellAdminUITest {

    /**
     * Initializes Espresso Intents and stubs outgoing admin activities before each test.
     */
    @Before
    public void setUp() {
        Intents.init();

        intending(hasComponent(AdminEventsActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(AdminProfilesActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(AdminImagesActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
        intending(hasComponent(AdminNotificationsActivity.class.getName()))
                .respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    /**
     * Releases Espresso Intents after each test to avoid interference.
     */
    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Checks that all four section buttons (Profiles, Events, Images, Notifications)
     * are visible on the Admin main menu.
     */
    @Test
    public void mainMenu_allFourSectionButtons_areDisplayed() {
        try (ActivityScenario<AdminMainMenuActivity> ignored =
                     ActivityScenario.launch(AdminMainMenuActivity.class)) {
            onView(withId(R.id.btnAllProfiles)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAllEvents)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAllImages)).check(matches(isDisplayed()));
            onView(withId(R.id.btnAllNotifications)).check(matches(isDisplayed()));
        }
    }

    /**
     * Verifies that tapping the "All Events" button launches AdminEventsActivity.
     */
    @Test
    public void mainMenu_allEvents_launchesAdminEventsActivity() {
        try (ActivityScenario<AdminMainMenuActivity> ignored =
                     ActivityScenario.launch(AdminMainMenuActivity.class)) {
            onView(withId(R.id.btnAllEvents)).perform(click());
            intended(hasComponent(AdminEventsActivity.class.getName()));
        }
    }
    /**
     * Verifies that tapping the "All Profiles" button launches AdminProfilesActivity.
     */
    @Test
    public void mainMenu_allProfiles_launchesAdminProfilesActivity() {
        try (ActivityScenario<AdminMainMenuActivity> ignored =
                     ActivityScenario.launch(AdminMainMenuActivity.class)) {
            onView(withId(R.id.btnAllProfiles)).perform(click());
            intended(hasComponent(AdminProfilesActivity.class.getName()));
        }
    }

    /**
     * Verifies that tapping the "All Images" button launches AdminImagesActivity.
     */
    @Test
    public void mainMenu_allImages_launchesAdminImagesActivity() {
        try (ActivityScenario<AdminMainMenuActivity> ignored =
                     ActivityScenario.launch(AdminMainMenuActivity.class)) {
            onView(withId(R.id.btnAllImages)).perform(click());
            intended(hasComponent(AdminImagesActivity.class.getName()));
        }
    }

    /**
     * Verifies that tapping the "All Notifications" button launches AdminNotificationsActivity.
     */
    @Test
    public void mainMenu_allNotifications_launchesAdminNotificationsActivity() {
        try (ActivityScenario<AdminMainMenuActivity> ignored =
                     ActivityScenario.launch(AdminMainMenuActivity.class)) {
            onView(withId(R.id.btnAllNotifications)).perform(click());
            intended(hasComponent(AdminNotificationsActivity.class.getName()));
        }
    }

    /**
     * Checks that the AdminEventRemovedActivity displays the "Event Removed" banner
     * and the back button.
     */
    @Test
    public void eventRemoved_confirmationBannerAndBackButton_areDisplayed() {
        try (ActivityScenario<AdminEventRemovedActivity> ignored =
                     ActivityScenario.launch(AdminEventRemovedActivity.class)) {
            onView(withText("✔  Event Removed")).check(matches(isDisplayed()));
            onView(withId(R.id.backBtn)).check(matches(isDisplayed()));
        }
    }

    /**
     * Checks that the AdminProfileRemovedActivity displays the "Profile Removed" banner
     * and the back button.
     */
    @Test
    public void profileRemoved_confirmationBannerAndBackButton_areDisplayed() {
        try (ActivityScenario<AdminProfileRemovedActivity> ignored =
                     ActivityScenario.launch(AdminProfileRemovedActivity.class)) {
            onView(withText("✔  Profile Removed")).check(matches(isDisplayed()));
            onView(withId(R.id.backBtn)).check(matches(isDisplayed()));
        }
    }

    /**
     * Checks that the AdminImageRemovedActivity displays the "Image Removed" banner
     * and the back button.
     */

    @Test
    public void imageRemoved_confirmationBannerAndBackButton_areDisplayed() {
        try (ActivityScenario<AdminImageRemovedActivity> ignored =
                     ActivityScenario.launch(AdminImageRemovedActivity.class)) {
            onView(withText("✔  Image Removed")).check(matches(isDisplayed()));
            onView(withId(R.id.backBtn)).check(matches(isDisplayed()));
        }
    }
}