package com.example.waitwell;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationModel> notifications;
    private Context context;

    public NotificationAdapter(List<NotificationModel> notifications) {
        this.notifications = notifications;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.notification_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        NotificationModel n = notifications.get(position);

        holder.actionButton.setText(n.getButtonLabel());

        holder.actionButton.setOnClickListener(v -> {
            Intent intent;

            // Navigate based on notification type
            if (n.getType() == NotificationModel.NotificationType.CHOSEN) {
                // Go to the chosen accept/decline screen
                intent = new Intent(context, EntrantChosenAccept.class);
            } else {
                // Go to the not chosen screen (re-enter lottery option)
                intent = new Intent(context, EntrantNotChosenScreen.class);
            }

            // Pass notification data to the target activity
            intent.putExtra("eventName", n.getEventName());
            intent.putExtra("message", n.getMessage());
            

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
