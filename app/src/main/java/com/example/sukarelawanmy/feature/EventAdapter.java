package com.example.sukarelawanmy.feature;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.EventItemBinding;
import com.example.sukarelawanmy.model.Event;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private final OnItemClickListener listener;

    private List<Event> events;
    private FirebaseFirestore db;
    private String currentUserId;

    public interface OnItemClickListener {
        void onItemClick(Event event);
        void onJoinClicked(Event event);
    }

    public EventAdapter(FirebaseFirestore db, String currentUserId, List<Event> events, OnItemClickListener listener) {
        this.events = events;
        this.listener = listener;
        this.db = db;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EventItemBinding itemBinding = EventItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EventViewHolder(itemBinding);
    }



    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.binding.eventTitle.setText(event.getTitle());
        holder.binding.eventLocation.setText(event.getLocation());
        // Handle the date string conversion
        String dateString = event.getDate();
        // In your EventAdapter's onBindViewHolder:
        if (event.isRequiresApproval()) {
            // Check participant status
            db.collection("event_participants")
                    .whereEqualTo("eventId", event.getEventId())
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String status = querySnapshot.getDocuments().get(0).getString("status");
                            switch (status) {
                                case "pending":
                                    holder.binding.joinButton.setText("Pending Approval");
                                    holder.binding.joinButton.setEnabled(false);
                                    break;
                                case "approved":
                                    holder.binding.joinButton.setText("Joined");
                                    holder.binding.joinButton.setEnabled(false);
                                    break;
                                case "rejected":
                                    holder.binding.joinButton.setText("Rejected");
                                    holder.binding.joinButton.setEnabled(false);
                                    break;
                            }
                        }
                    });
        }

        if (dateString != null && !dateString.isEmpty()) {
            try {
                // First parse the string to Date object
                // Adjust the input pattern according to your actual date string format
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = inputFormat.parse(dateString);

                // Then format to desired output
                SimpleDateFormat outputFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
                holder.binding.eventDate.setText(outputFormat.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
                // Fallback: display the raw string if parsing fails
                holder.binding.eventDate.setText(dateString);
            }
        } else {
            holder.binding.eventDate.setText("No date");
        }
        Glide.with(holder.itemView.getContext())
                .load("https://imgur.com/a/Rz31kKa") // sample image URL
                .placeholder(R.drawable.placeholder_image) // optional
                .error(R.drawable.placeholder_image) // optional
                .into(holder.binding.image);
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            listener.onItemClick(event);
        });
        holder.binding.joinButton.setOnClickListener(v -> {
            listener.onJoinClicked(event);
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        EventItemBinding binding;

        EventViewHolder(EventItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

        }

    }
}