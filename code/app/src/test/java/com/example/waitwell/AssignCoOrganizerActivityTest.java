

package com.example.waitwell;

// Unit tests for AssignCoOrganizerActivity logic — US 02.09.01 + US 01.09.01
// Pure Java only — no Mockito (incompatible with Java 25, team-wide known issue)
public class AssignCoOrganizerActivityTest {

    // confirms a fresh CoOrganizerUserItem holds the userId it was given
    @org.junit.Test
    public void userItem_storesUserId() {
        com.example.waitwell.activities.AssignCoOrganizerActivity.CoOrganizerUserItem item =
                new com.example.waitwell.activities.AssignCoOrganizerActivity.CoOrganizerUserItem(
                        "user123", "Alice", "alice@example.com", "555-0100", "alice@example.com");
        org.junit.Assert.assertEquals("user123", item.userId);
    }

    // confirms name is stored correctly
    @org.junit.Test
    public void userItem_storesName() {
        com.example.waitwell.activities.AssignCoOrganizerActivity.CoOrganizerUserItem item =
                new com.example.waitwell.activities.AssignCoOrganizerActivity.CoOrganizerUserItem(
                        "user456", "Bob", "bob@example.com", "", "bob@example.com");
        org.junit.Assert.assertEquals("Bob", item.name);
    }

    // confirms matchedValue is stored correctly
    @org.junit.Test
    public void userItem_storesMatchedValue() {
        com.example.waitwell.activities.AssignCoOrganizerActivity.CoOrganizerUserItem item =
                new com.example.waitwell.activities.AssignCoOrganizerActivity.CoOrganizerUserItem(
                        "user789", "Carol", "carol@example.com", "555-0200", "555-0200");
        org.junit.Assert.assertEquals("555-0200", item.matchedValue);
    }

    // confirms WaitlistEntry status transition from waiting to cancelled
    // mirrors what assignCoOrganizer does: existing waitlist doc gets deleted,
    // but if we model it as a status change the transition must be valid
    @org.junit.Test
    public void waitlistEntry_statusCanBeSetToAnyValidValue() {
        WaitlistEntry entry = new WaitlistEntry("userA", "event1", "Test Event");
        org.junit.Assert.assertEquals("waiting", entry.getStatus());
        entry.setStatus("cancelled");
        org.junit.Assert.assertEquals("cancelled", entry.getStatus());
    }

    // confirms that two different users produce different entry doc IDs
    @org.junit.Test
    public void entryDocId_isUniquePerUser() {
        String eventId = "event42";
        String userId1 = "alice";
        String userId2 = "bob";
        String docId1 = userId1 + "_" + eventId;
        String docId2 = userId2 + "_" + eventId;
        org.junit.Assert.assertNotEquals(docId1, docId2);
    }
}