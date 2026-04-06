# potato1-wait-well
CMPUT 301 W26 
Project github repository for Team :
Potato1

## Extra features implemented

This section lists the features we are claiming for the **optional bonus marks (+2%)** for implementing extra functionality beyond the core project requirements.

- **Calendar view** — View a calendar of all events and all registered events, using the device’s time zone.
- **Carousel on the main screen** — Browse events in a carousel on the main screen; other UI components can be added as well.

## Extra Implementations Beyond Project Scope

The following features were implemented in addition to the minimum required user stories. These were not part of the original project requirements but were added to improve the overall user experience and completeness of the app.

Scope note: required stories were taken as the US 01.xx.xx, US 02.xx.xx, and related flows that the codebase already maps to user-story comments (for example waitlist, QR, organizer event lifecycle, lottery, notifications, co-organizer, and CSV export where tagged). Anything below is extra on top of that baseline.

- **Entrant: Featured event carousel** — On the home screen, public events appear as a horizontal row of cards (with poster images when stored in Firebase Storage), and tapping a card opens event details.
- **Entrant: Popular events tabs** — The home screen can switch the short list under the cards between “most viewed” (by stored view count) and “latest” (by creation time).
- **Entrant: View count** — Opening an event from a home card increments that event’s `viewCount` in Firestore so “most viewed” can rank events.
- **Entrant: Registration countdown and deadline-based status** — On event detail, a registration line counts down how many days are left before the registration deadline (including “closes today” and “one more day”), then shows “Registration closed” or “Event completed” once the app’s date logic says registration or the event itself is over, and the join button is turned off with a clear closed state. On home and event-browsing lists, each row shows a coloured pill for **Open**, **Closed**, or **Completed** so entrants can see at a glance whether an event is still accepting registration by deadline.
- **Entrant: Calendar view** — A dedicated calendar screen builds a month grid from event dates, shows how many events fall on each day, supports changing month/year, and lists the events for the day you tap (dates follow the device’s default locale and calendar).
- **Entrant: Registration history** — A screen lists past outcomes (selected, confirmed, or rejected) from the entrant’s waitlist entries and links each row to the event detail page, with the same bottom navigation pattern as other entrant screens.
- **Entrant: Notification cards by category** — The notifications list assigns each item a category (for example invitation, reminder, confirmed, rejected, cancelled, co-organizer), and updates the section title, accent colour, and icon to match.
- **Entrant: Expired invitations** — Chosen-invite notifications are treated as expired when the event is completed or when the registration deadline has passed; those rows are visually muted and tapping them shows an expiry toast instead of the normal flow.
- **Entrant: No double response on invites** — Before opening accept/decline, the app checks the waitlist entry (and co-organizer response state) so users who already confirmed, cancelled, or answered a co-organizer invite get a read-only path instead of acting twice.
- **Entrant: Blocked waitlist re-join** — Joining a waitlist is rejected in the Firestore transaction if the entrant already has a waitlist entry in a terminal state (confirmed, selected, rejected, or cancelled) for that event.
- **Organiser: Share from manage event** — On a public event’s manage screen, Share loads the event and switches to the same QR / “event created” style view used after creating an event, so organizers can get to the QR flow without going back through creation.
- **Organiser: Filter invited entrants by status** — On the invited entrants screen, organizers can use checkboxes to show who is enrolled (accepted), who is still invited/pending selection, who has cancelled, and combinations of those, plus search the list by text so they can focus on one group before sending notifications or removing applicants.
- **Organiser: Waitlist screen tools** — From “view requests,” organizers can search the waiting list, open a profile preview for an entrant, accept or decline individuals, send a notification to everyone still on the waitlist, start the lottery sampling flow, and open a map of where entrants joined when location was collected for the event.
- **Organiser: Entrant profile preview** — On several organizer entrant lists (waitlist requests, invited entrants, sampled entrants after the lottery, final entrants, and cancelled entrants), tapping the view/profile control opens a dialog that loads that user from Firestore and shows extra detail beyond the row: display name, formatted account join date, and a circular profile photo when `profileImageUrl` is set.
- **Organiser: After the lottery** — A sampling confirmation screen summarizes how many entrants were chosen, then a sampled-entrants screen lists everyone in the “selected” state with search, profile preview, and removing someone from the sampled list behind a confirmation dialog; the event detail flow can also draw a single replacement applicant after the lottery.
- **Organiser: Final, enrolled, and cancelled lists** — Separate screens list final entrants (with search and profile preview) and let the organizer export that list as a CSV file and share it through another app via `FileProvider`; another screen lists enrolled entrants so organizers can cancel no-shows with confirmation; cancelled entrants have their own list with bulk notify for that group.
- **Organiser: Delete event cleanup** — Deleting an event triggers a batched cleanup that removes related waitlist entries, notifications, registrations, comments, and deletes associated poster files in Firebase Storage when present.
