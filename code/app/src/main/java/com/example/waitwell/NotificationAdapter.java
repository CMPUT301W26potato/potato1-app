package com.example.waitwell;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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

        holder.actionButton.setOnClickListener(v -> {
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

    @Override
    public int getItemCount() { return notifications.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        Button actionButton;

        public ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            actionButton = itemView.findViewById(R.id.actionButton);
        }
    }
}
