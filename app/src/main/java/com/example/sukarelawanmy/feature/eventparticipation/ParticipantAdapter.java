package com.example.sukarelawanmy.feature.eventparticipation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.ItemParticiptionBinding;
import com.example.sukarelawanmy.model.EventParticipation;

import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    private final List<EventParticipation> participants;
    private final ParticipantClickListener listener;

    public interface ParticipantClickListener {
        void onCancelParticipation(EventParticipation participation);
        void onParticipantClick(EventParticipation participation);
    }

    public ParticipantAdapter(List<EventParticipation> participants, ParticipantClickListener listener) {
        this.participants = participants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemParticiptionBinding binding = ItemParticiptionBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ParticipantViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        EventParticipation participant = participants.get(position);
        holder.bind(participant);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private final ItemParticiptionBinding binding;

        public ParticipantViewHolder(@NonNull ItemParticiptionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(EventParticipation participant) {
            // Set participant data
            binding.participantName.setText(participant.getUserName());

//            // Load profile image
//            Glide.with(itemView.getContext())
//                    .load(participant.getUserProfileImage())
//                    .placeholder(R.drawable.ic_profile_placeholder)
//                    .into(binding.participantImage);

            // Set status
            binding.joinStatus.setText(participant.getStatus());
            int statusColor = getStatusColor(participant.getStatus());
            binding.joinStatus.setTextColor(statusColor);
            binding.joinStatus.setBackgroundResource(getStatusBackground(participant.getStatus()));


            // Handle date formatting safely
            if (participant.getJoinedAt() != null) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
                    String joinDate = "Joined: " + dateFormat.format(participant.getJoinedAt().toDate());
                    binding.joinDateTime.setText(joinDate);
                } catch (Exception e) {
                    Log.e("ParticipantAdapter", "Error formatting date", e);
                    binding.joinDateTime.setText("Joined: Date not available");
                }
            } else {
                binding.joinDateTime.setText("Joined: Date not available");
            }


            // Set skills
            binding.participantSkills.setText(formatSkills(participant.getUserSkills()));

            // Set address
            binding.participantAddress.setText("Address"+participant.getUserAddress());

            // Set click listeners
            binding.btnCancelParticipation.setOnClickListener(v ->
                    listener.onCancelParticipation(participant));

            itemView.setOnClickListener(v ->
                    listener.onParticipantClick(participant));
        }

        private String formatSkills(List<String> skills) {
            if (skills == null || skills.isEmpty()) return "No skills listed";
            return String.join(", ", skills);
        }

        private int getStatusColor(String status) {
            switch (status.toLowerCase()) {
                case "approved": return itemView.getContext().getColor(R.color.dark_text);
                case "pending": return itemView.getContext().getColor(R.color.orange);
                case "rejected": return itemView.getContext().getColor(R.color.orange);
                default: return itemView.getContext().getColor(R.color.primaryColor);
            }
        }

        private int getStatusBackground(String status) {
            switch (status.toLowerCase()) {
                case "approved": return R.drawable.bg_status_approved;
                case "pending": return R.drawable.bg_status_pending;
                case "rejected": return R.drawable.bg_status_rejected;
                default: return R.drawable.bg_status_default;
            }
        }
    }
}