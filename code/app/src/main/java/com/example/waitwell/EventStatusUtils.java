package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;

import java.util.Calendar;
import java.util.Date;

/**
 * Derives event lifecycle status from dates (not only the stored {@code status} field).
 * <ul>
 *   <li><b>Open</b> – registration deadline has not passed yet.</li>
 *   <li><b>Closed</b> – deadline has passed, but the event day has not passed yet.</li>
 *   <li><b>Completed</b> – calendar day is after the event date.</li>
 * </ul>
 *
 * Addresses: US 02.01.04 - Organizer: Set Registration Period, US 01.05.07 - Entrant: Accept/Decline Private Event
 *
 * @author Karina Zhang
 * @version 1.0
 */
public final class EventStatusUtils {

    private EventStatusUtils() {
    }

    /** Firestore field on {@code events} for the registration deadline (see {@link com.example.waitwell.Event}).
     *
     * @author Karina Zhang
     */
    public static final String FIELD_REGISTRATION_CLOSE = "registrationClose";

    /**
     * Computes lifecycle status from a Firestore event doc.
     *
     * @param doc event document snapshot
     * @return "open", "closed", or "completed"
     * @author Karina Zhang
     */
    public static String computeStatus(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return "closed";
        }
        return computeStatus(
                doc.getDate("eventDate"),
                getRegistrationCloseDate(doc),
                new Date());
    }

    /**
     * End of registration as stored on the event document ({@value #FIELD_REGISTRATION_CLOSE}).
     *
     * @param doc event document snapshot
     * @return registration close date or null if missing
     * @author Karina Zhang
     */
    public static Date getRegistrationCloseDate(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        Date d = doc.getDate(FIELD_REGISTRATION_CLOSE);
        if (d != null) {
            return d;
        }
        Timestamp ts = doc.getTimestamp(FIELD_REGISTRATION_CLOSE);
        return ts != null ? ts.toDate() : null;
    }

    /**
     * @param eventDate        when the event happens (organizer "Date of event"); may be null for legacy docs
     * @param registrationClose end of registration; may be null
     * @param now              typically {@code new Date()}
     * @return computed lifecycle status string
     * @author Karina Zhang
     */
    public static String computeStatus(Date eventDate, Date registrationClose, Date now) {
        if (eventDate != null && now.after(eventDate)) {
            return "completed";
        }
        if (registrationClose != null && registrationClose.before(now)) {
            return "closed";
        }
        return "open";
    }

    /**
     * True if {@code now} falls on a calendar day strictly after {@code eventDate}'s day.
     *
     * @param now current date/time
     * @param eventDate event date to compare against
     * @return true when current day is after event day
     * @author Karina Zhang
     */
    public static boolean isCalendarDayAfter(Date now, Date eventDate) {
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(eventDate);
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTime(now);
        int ey = eventCal.get(Calendar.YEAR);
        int em = eventCal.get(Calendar.MONTH);
        int ed = eventCal.get(Calendar.DAY_OF_MONTH);
        int ny = nowCal.get(Calendar.YEAR);
        int nm = nowCal.get(Calendar.MONTH);
        int nd = nowCal.get(Calendar.DAY_OF_MONTH);
        if (ny != ey) {
            return ny > ey;
        }
        if (nm != em) {
            return nm > em;
        }
        return nd > ed;
    }

    /**
     * True if {@code a}'s calendar day is strictly before {@code b}'s calendar day.
     *
     * @param a first date
     * @param b second date
     * @return true when date a is before date b by day
     * @author Karina Zhang
     */
    public static boolean isCalendarDayBefore(Date a, Date b) {
        Calendar ca = Calendar.getInstance();
        ca.setTime(a);
        Calendar cb = Calendar.getInstance();
        cb.setTime(b);
        int ay = ca.get(Calendar.YEAR), am = ca.get(Calendar.MONTH), ad = ca.get(Calendar.DAY_OF_MONTH);
        int by = cb.get(Calendar.YEAR), bm = cb.get(Calendar.MONTH), bd = cb.get(Calendar.DAY_OF_MONTH);
        if (ay != by) {
            return ay < by;
        }
        if (am != bm) {
            return am < bm;
        }
        return ad < bd;
    }

    /** Local calendar start of today (00:00:00).
     *
     * @return today at local midnight
     * @author Karina Zhang
     */
    public static Date startOfToday() {
        return startOfDay(new Date());
    }

    /** Local midnight on the same calendar day as {@code d}.
     *
     * @param d source date
     * @return that same day at local midnight
     * @author Karina Zhang
     */
    public static Date startOfDay(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
