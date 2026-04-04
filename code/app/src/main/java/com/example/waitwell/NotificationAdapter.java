package com.example.waitwell;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
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

import java.util.List;

/**
 *  connect the notifications to the recycler view / ui
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

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
            holder.itemView.setAlpha(1f);
        }

        holder.actionButton.setOnClickListener(v -> {
            if (n.isExpired() && n.getType() == NotificationModel.NotificationType.CHOSEN) {
                Toast.makeText(context, R.string.toast_notification_invitation_expired, Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent;
            //navigations based on the notification type
            if (n.getType() == NotificationModel.NotificationType.CHOSEN) {
                intent = new Intent(context, InvitationResponseActivity.class);
            } else {
                intent = new Intent(context, EntrantNotChosenScreen.class);
            }

            intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_ID, n.getEventId());
            intent.putExtra(InvitationResponseActivity.EXTRA_EVENT_NAME, n.getEventName());
            intent.putExtra(InvitationResponseActivity.EXTRA_MESSAGE, n.getMessage());

            if (parentActivity != null) {
                String notificationId = parentActivity.getNotificationId(position);
                if (notificationId != null) {
                    intent.putExtra(InvitationResponseActivity.EXTRA_NOTIFICATION_ID, notificationId);
                }
            }

            context.startActivity(intent);
        });
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
