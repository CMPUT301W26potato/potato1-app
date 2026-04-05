package com.example.waitwell;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.NonNull;

/**
 * Maps an in-memory {@link NotificationModel} (built from existing Firestore fields:
 * {@code type}, {@code message}, {@code eventName}, etc.) to a display-only category for
 * entrant notification list visuals.
 */
public final class NotificationDisplayCategorizer {

    public enum NotificationItemCategory {
        INVITATION,
        REMINDER,
        CONFIRMED,
        REJECTED,
        CANCELLED,
        // REHAAN'S ADDITION — US 02.09.01 Part 2
        CO_ORGANIZER,
        // END REHAAN'S ADDITION
        GENERAL
    }
    /**
     * Categorizes using only data already present on the notification document
     * (exposed through {@link NotificationModel}: message body and coarse type).
     */
    @NonNull
    public static NotificationItemCategory getNotificationType(@NonNull Context context,
                                                                @NonNull NotificationModel model) {
        String message = model.getMessage();
        String eventName = model.getEventName();
        if (eventName == null) {
            eventName = "";
        }

// REHAAN'S ADDITION — CO_ORGANIZER type (US 02.09.01 Part 2)
        if (model.getType() == NotificationModel.NotificationType.CO_ORGANIZER) {
            return NotificationItemCategory.CO_ORGANIZER;
        }
        // END REHAAN'S ADDITION

        if (model.getType() == NotificationModel.NotificationType.CHOSEN) {
            if (messageMatches(context, R.string.invited_notify_pending_message, eventName, message)) {
                return NotificationItemCategory.REMINDER;
            }
            if (messageMatches(context, R.string.invited_notify_confirmed_message, eventName, message)) {
                return NotificationItemCategory.CONFIRMED;
            }
            if (messageMatches(context, R.string.co_organizer_notification_message, eventName, message)) {
                return NotificationItemCategory.GENERAL;
            }
            return NotificationItemCategory.INVITATION;
        }

        if (messageMatches(context, R.string.invited_notify_cancelled_message, eventName, message)) {
            return NotificationItemCategory.CANCELLED;
        }
        if (messageMatches(context, R.string.notification_entrant_not_selected, eventName, message)) {
            return NotificationItemCategory.REJECTED;
        }
        if (context.getString(R.string.event_detail_registration_not_accepted).equals(message)) {
            return NotificationItemCategory.REJECTED;
        }
        if (messageMatches(context, R.string.waitlist_notify_all_message, eventName, message)) {
            return NotificationItemCategory.GENERAL;
        }

        return NotificationItemCategory.GENERAL;
    }

    private static boolean messageMatches(Context context, int templateRes, String eventName,
                                          String actualMessage) {
        if (TextUtils.isEmpty(actualMessage)) {
            return false;
        }
        String expected = context.getString(templateRes, eventName);
        return expected.equals(actualMessage);
    }
}
