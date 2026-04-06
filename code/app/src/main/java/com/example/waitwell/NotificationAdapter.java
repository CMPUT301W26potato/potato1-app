package com.example.waitwell;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waitwell.activities.InvitationResponseActivity;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Recycler adapter for entrant notifications so each card gets the right style and click action.
 * This is part of the private invite notification and accept/decline flow.
 *
 * Addresses: US 01.05.06 - Entrant: Private Event Invite Notification, US 01.05.07 - Entrant: Accept/Decline Private Event
 *
 * @author Karina Zhang
 * @version 1.0
 * @see com.example.waitwell.activities.InvitationResponseActivity
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    /*
     * Asked Gemini how to query Firestore for a specific user's notifications
     * and how to sort them by timestamp properly. Also used it to think through
     * how to update the UI when a notification gets tapped without messing up
     * the rest of the list.
     * approach.
     *
     * Sites I looked at:
     *
     * Firestore queries - how whereEqualTo and orderBy work together:
     * https://firebase.google.com/docs/firestore/query-data/queries
     *
     * RecyclerView with Firestore data - binding patterns:
     * https://developer.android.com/reference/com/firebase/ui/firestore/FirestoreRecyclerAdapter
     *
     * RecyclerView click listeners in an adapter:
     * https://developer.android.com/guide/topics/ui/layout/recyclerview#click-listener
     */

    private List<NotificationModel> notifications;
    private Context context;
    private EntrantNotificationScreen parentActivity;  // To get notification IDs

    public NotificationAdapter(List<NotificationModel> notifications) {
        this.notifications = notifications;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        // Try to get parent activity for notification IDs
        if (context instanceof EntrantNotificationScreen) {
            parentActivity = (EntrantNotificationScreen) context;
        }
        View view = LayoutInflater.from(context)
                .inflate(R.layout.notification_card, parent, false);
        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        NotificationModel n = notifications.get(position);

        holder.actionButton.setText(n.getButtonLabel());
        holder.titleText.setText(n.getEventName());
        String body = n.getMessage();
        holder.messageText.setText(body != null ? body : "");

        NotificationDisplayCategorizer.NotificationItemCategory category =
                NotificationDisplayCategorizer.getNotificationType(context, n);
        CategoryStyle style = categoryStyleFor(category);

        holder.categoryTitleText.setText(style.titleRes);
        holder.leftBorderStrip.setBackgroundColor(style.accentColor);
        holder.iconContainer.setBackgroundResource(R.drawable.bg_waitlist_icon_dark);
        holder.iconContainer.setBackgroundTintList(ColorStateList.valueOf(style.accentColor));
        @ColorInt int iconOnAccent = ContextCompat.getColor(context, R.color.text_white);
        tintIcon(holder.imgNotificationTypeIcon, style.iconRes, iconOnAccent);

        @ColorInt int primary = ContextCompat.getColor(context, R.color.text_primary);
        @ColorInt int secondary = ContextCompat.getColor(context, R.color.text_secondary);
        holder.titleText.setTextColor(primary);
        holder.categoryTitleText.setTextColor(primary);
        holder.messageText.setTextColor(secondary);

        if (n.isExpired()) {
            holder.expiredLabel.setVisibility(View.VISIBLE);
            holder.itemView.setAlpha(0.5f);
            @ColorInt int muted = ContextCompat.getColor(context, R.color.text_secondary);
            holder.titleText.setTextColor(muted);
            holder.categoryTitleText.setTextColor(muted);
            holder.messageText.setTextColor(muted);
            @ColorInt int hint = ContextCompat.getColor(context, R.color.text_hint);
            holder.leftBorderStrip.setBackgroundColor(hint);
            holder.iconContainer.setBackgroundTintList(ColorStateList.valueOf(hint));
            tintIcon(holder.imgNotificationTypeIcon, style.iconRes, muted);
        } else {
            holder.expiredLabel.setVisibility(View.GONE);
            if (n.isInviteAlreadyResolvedOnWaitlist()
                    || (n.getType() == NotificationModel.NotificationType.CO_ORGANIZER && n.isResponded())) {
                @ColorInt int hint = ContextCompat.getColor(context, R.color.text_hint);
                holder.leftBorderStrip.setBackgroundColor(hint);
                holder.iconContainer.setBackgroundTintList(ColorStateList.valueOf(hint));
                holder.actionButton.setBackgroundTintList(ColorStateList.valueOf(hint));
                holder.itemView.setAlpha(0.5f);
            } else {
                holder.actionButton.setBackgroundTintList(null);
                holder.itemView.setAlpha(1f);
            }
        }

        if (category == NotificationDisplayCategorizer.NotificationItemCategory.CONFIRMED) {
            holder.actionButton.setVisibility(View.GONE);
        } else {
            holder.actionButton.setVisibility(View.VISIBLE);
        }

        maybeFetchInviteWaitlistStatus(holder, position, n, category);

        holder.actionButton.setOnClickListener(v -> onNotificationActionClick(holder, n));
    }

    private void maybeFetchInviteWaitlistStatus(ViewHolder holder, int position, NotificationModel n,
                                                NotificationDisplayCategorizer.NotificationItemCategory category) {
        boolean inviteLike = category == NotificationDisplayCategorizer.NotificationItemCategory.INVITATION
                || category == NotificationDisplayCategorizer.NotificationItemCategory.REMINDER;
        if (n.isExpired()
                || n.getType() != NotificationModel.NotificationType.CHOSEN
                || !inviteLike
                || TextUtils.isEmpty(n.getEventId())
                || n.isInviteWaitlistStatusFetchStarted()) {
            return;
        }
        n.setInviteWaitlistStatusFetchStarted(true);
        final int pos = position;
        final NotificationModel modelRef = n;
        final String waitlistCollection = "waitlist_entries";
        final String fieldStatus = "status";
        String uid = DeviceUtils.getDeviceId(context);
        String entryDocId = uid + "_" + n.getEventId();
        String confirmed = context.getString(R.string.firestore_waitlist_status_confirmed);
        String cancelled = context.getString(R.string.firestore_waitlist_status_cancelled);
        FirebaseFirestore.getInstance()
                .collection(waitlistCollection)
                .document(entryDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        return;
                    }
                    String st = task.getResult().getString(fieldStatus);
                    if (confirmed.equals(st) || cancelled.equals(st)) {
                        modelRef.setInviteAlreadyResolvedOnWaitlist(true);
                    }
                    holder.itemView.post(() -> {
                        if (pos < 0 || pos >= notifications.size() || notifications.get(pos) != modelRef) {
                            return;
                        }
                        NotificationAdapter.this.notifyItemChanged(pos);
                    });
                });
    }

    private void onNotificationActionClick(ViewHolder holder, NotificationModel n) {
        // REHAAN'S ADDITION â€” CO_ORGANIZER routing (US 02.09.01 Part 2)
        if (n.getType() == NotificationModel.NotificationType.CO_ORGANIZER) {
            if (n.isResponded()) {
                // Already responded — check event to determine accepted vs declined
                String uid = DeviceUtils.getDeviceId(context);
                FirebaseFirestore.getInstance()
                        .collection("events").document(n.getEventId())
                        .get()
                        .addOnCompleteListener(task -> {
                            String status;
                            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                java.util.List<String> coOrgIds = (java.util.List<String>) task.getResult().get("coOrganizerIds");
                                status = (coOrgIds != null && coOrgIds.contains(uid)) ? "accepted" : "declined";
                            } else {
                                status = "declined";
                            }
                            holder.itemView.post(() -> {
                                Intent intent = new Intent(context, com.example.waitwell.activities.CoOrganizerInviteResponseActivity.class);
                                intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_EVENT_ID, n.getEventId());
                                intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_EVENT_NAME, n.getEventName());
                                intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_MESSAGE, n.getMessage());
                                intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_ALREADY_RESPONDED, status);
                                context.startActivity(intent);
                            });
                        });
            } else {
                Intent intent = new Intent(context, com.example.waitwell.activities.CoOrganizerInviteResponseActivity.class);
                intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_EVENT_ID, n.getEventId());
                intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_EVENT_NAME, n.getEventName());
                intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_MESSAGE, n.getMessage());
                if (parentActivity != null) {
                    int pos = holder.getBindingAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        String notifId = parentActivity.getNotificationId(pos);
                        if (notifId != null) {
                            intent.putExtra(com.example.waitwell.activities.CoOrganizerInviteResponseActivity.EXTRA_NOTIFICATION_ID, notifId);
                        }
                    }
                }
                context.startActivity(intent);
            }
            return;
        }
        // END REHAAN'S ADDITION

        if (n.isExpired() && n.getType() == NotificationModel.NotificationType.CHOSEN) {
            Toast.makeText(context, R.string.toast_notification_invitation_expired, Toast.LENGTH_SHORT).show();
            return;
        }
        if (n.getType() == NotificationModel.NotificationType.CHOSEN) {
            if (TextUtils.isEmpty(n.getEventId())) {
                startChosenInvitationActivity(n, holder.getBindingAdapterPosition());
                return;
            }
            final int pos = holder.getBindingAdapterPosition();
            final String waitlistCollection = "waitlist_entries";
            final String fieldStatus = "status";
            String uid = DeviceUtils.getDeviceId(context);
            String entryDocId = uid + "_" + n.getEventId();
            String confirmed = context.getString(R.string.firestore_waitlist_status_confirmed);
            String cancelled = context.getString(R.string.firestore_waitlist_status_cancelled);
            FirebaseFirestore.getInstance()
                    .collection(waitlistCollection)
                    .document(entryDocId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                            holder.itemView.post(() -> startChosenInvitationActivity(n, pos));
                            return;
                        }
                        String st = task.getResult().getString(fieldStatus);
                        if (confirmed.equals(st)) {
                            holder.itemView.post(() -> startChosenInvitationReadOnly(n, pos, confirmed));
                            return;
                        }
                        if (cancelled.equals(st)) {
                            holder.itemView.post(() -> startChosenInvitationReadOnly(n, pos, cancelled));
                            return;
                        }
                        holder.itemView.post(() -> startChosenInvitationActivity(n, pos));
                    });
            return;
        }
        Intent intent = new Intent(context, EntrantNotChosenScreen.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_ID, n.getEventId());
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, n.getEventName());
        intent.putExtra(InvitationResponseActivity.EXTRA_MESSAGE, n.getMessage());
        if (parentActivity != null) {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                String notificationId = parentActivity.getNotificationId(pos);
                if (notificationId != null) {
                    intent.putExtra(InvitationResponseActivity.EXTRA_NOTIFICATION_ID, notificationId);
                }
            }
        }
        context.startActivity(intent);
    }

    private void startChosenInvitationActivity(NotificationModel n, int position) {
        Intent intent = new Intent(context, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_ID, n.getEventId());
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, n.getEventName());
        intent.putExtra(InvitationResponseActivity.EXTRA_MESSAGE, n.getMessage());
        if (parentActivity != null && position != RecyclerView.NO_POSITION) {
            String notificationId = parentActivity.getNotificationId(position);
            if (notificationId != null) {
                intent.putExtra(InvitationResponseActivity.EXTRA_NOTIFICATION_ID, notificationId);
            }
        }
        context.startActivity(intent);
    }

    private void startChosenInvitationReadOnly(NotificationModel n, int position, String resolvedStatus) {
        Intent intent = new Intent(context, InvitationResponseActivity.class);
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_ID, n.getEventId());
        intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, n.getEventName());
        intent.putExtra(InvitationResponseActivity.EXTRA_ALREADY_RESPONDED, resolvedStatus);
        if (parentActivity != null && position != RecyclerView.NO_POSITION) {
            String notificationId = parentActivity.getNotificationId(position);
            if (notificationId != null) {
                intent.putExtra(InvitationResponseActivity.EXTRA_NOTIFICATION_ID, notificationId);
            }
        }
        context.startActivity(intent);
    }

    @NonNull
    private CategoryStyle categoryStyleFor(NotificationDisplayCategorizer.NotificationItemCategory category) {
        switch (category) {
            case REMINDER:
                return new CategoryStyle(
                        ContextCompat.getColor(context, R.color.primary),
                        R.drawable.ic_clock_16,
                        R.string.notification_category_reminder);
            case INVITATION:
                return new CategoryStyle(
                        ContextCompat.getColor(context, R.color.primary),
                        R.drawable.ic_notifications_selected,
                        R.string.notification_category_invitation);
            case CONFIRMED:
                return new CategoryStyle(
                        ContextCompat.getColor(context, R.color.primary),
                        R.drawable.ic_check_white,
                        R.string.notification_category_confirmed);
            case REJECTED:
                return new CategoryStyle(
                        ContextCompat.getColor(context, R.color.status_closed_text),
                        R.drawable.ic_delete,
                        R.string.notification_category_rejected);
            case CANCELLED:
                return new CategoryStyle(
                        ContextCompat.getColor(context, R.color.status_closed_text),
                        R.drawable.ic_minus_white,
                        R.string.notification_category_cancelled);
            // REHAAN'S ADDITION â€” US 02.09.01 Part 2
            case CO_ORGANIZER:
                return new CategoryStyle(
                        ContextCompat.getColor(context, R.color.primary),
                        R.drawable.ic_notifications_selected,
                        R.string.notification_category_co_organizer);
            // END REHAAN'S ADDITION
            case GENERAL:
            default:
                return new CategoryStyle(
                        ContextCompat.getColor(context, R.color.text_primary),
                        R.drawable.ic_search,
                        R.string.notification_category_general);
        }
    }

    private void tintIcon(@NonNull ImageView target, @DrawableRes int drawableRes, @ColorInt int tint) {
        Drawable base = AppCompatResources.getDrawable(context, drawableRes);
        if (base == null) {
            target.setImageDrawable(null);
            return;
        }
        Drawable wrapped = DrawableCompat.wrap(base.mutate());
        DrawableCompat.setTint(wrapped, tint);
        target.setImageDrawable(wrapped);
    }

    @Override
    public int getItemCount() { return notifications.size(); }

    private static final class CategoryStyle {
        @ColorInt final int accentColor;
        @DrawableRes final int iconRes;
        @StringRes final int titleRes;

        CategoryStyle(@ColorInt int accentColor, @DrawableRes int iconRes, @StringRes int titleRes) {
            this.accentColor = accentColor;
            this.iconRes = iconRes;
            this.titleRes = titleRes;
        }
    }

    /**
     * Holds view refs for one notification card row.
     *
     * Addresses: US 01.05.06 - Entrant: Private Event Invite Notification, US 01.05.07 - Entrant: Accept/Decline Private Event
     *
     * @author Karina Zhang
     * @version 1.0
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        /*
         * Asked Gemini how to query Firestore for a specific user's notifications
         * and how to sort them by timestamp properly. Also used it to think through
         * how to update the UI when a notification gets tapped without messing up
         * the rest of the list.
         * approach.
         *
         * Sites I looked at:
         *
         * Firestore queries - how whereEqualTo and orderBy work together:
         * https://firebase.google.com/docs/firestore/query-data/queries
         *
         * RecyclerView with Firestore data - binding patterns:
         * https://developer.android.com/reference/com/firebase/ui/firestore/FirestoreRecyclerAdapter
         *
         * RecyclerView click listeners in an adapter:
         * https://developer.android.com/guide/topics/ui/layout/recyclerview#click-listener
         */
        View leftBorderStrip;
        FrameLayout iconContainer;
        ImageView imgNotificationTypeIcon;
        TextView categoryTitleText;
        TextView titleText;
        TextView messageText;
        TextView expiredLabel;
        Button actionButton;

        public ViewHolder(View itemView) {
            super(itemView);
            leftBorderStrip = itemView.findViewById(R.id.leftBorderStrip);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            imgNotificationTypeIcon = itemView.findViewById(R.id.imgNotificationTypeIcon);
            categoryTitleText = itemView.findViewById(R.id.categoryTitleText);
            titleText = itemView.findViewById(R.id.titleText);
            messageText = itemView.findViewById(R.id.messageText);
            expiredLabel = itemView.findViewById(R.id.expiredLabel);
            actionButton = itemView.findViewById(R.id.actionButton);
        }
    }
}

