package com.example.waitwell;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import java.util.Date;

/**
 * Derives event lifecycle status from dates (not only the stored {@code status} field).
 * <ul>
 *   <li><b>Open</b> – registration deadline has not passed yet.</li>
 *   <li><b>Closed</b> – deadline has passed, but the event day has not passed yet.</li>
 *   <li><b>Completed</b> – calendar day is after the event date.</li>
 * </ul>
 */
public final class EventStatusUtils {

    private EventStatusUtils() {
    }

    public static String computeStatus(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return "closed";
        }
        return computeStatus(
                doc.getDate("eventDate"),
                doc.getDate("registrationClose"),
                new Date());
    }

    /**
     * @param eventDate        when the event happens (organizer "Date of event"); may be null for legacy docs
     * @param registrationClose end of registration; may be null
     * @param now              typically {@code new Date()}
     */
    public static String computeStatus(Date eventDate, Date registrationClose, Date now) {
        if (eventDate != null && isCalendarDayAfter(now, eventDate)) {
            return "completed";
        }
        if (registrationClose != null && registrationClose.before(now)) {
            return "closed";
        }
        return "open";
    }

    /**
     * True if {@code now} falls on a calendar day strictly after {@code eventDate}'s day.
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

    /** Local calendar start of today (00:00:00). */
    public static Date startOfToday() {
        return startOfDay(new Date());
    }

    /** Local midnight on the same calendar day as {@code d}. */
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
