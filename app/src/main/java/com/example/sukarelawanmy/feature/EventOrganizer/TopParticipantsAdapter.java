package com.example.sukarelawanmy.feature.EventOrganizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.model.UserModel;
import com.example.sukarelawanmy.model.UserParticipation;
import com.example.sukarelawanmy.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TopParticipantsAdapter extends RecyclerView.Adapter<TopParticipantsAdapter.ParticipantViewHolder> {

    private final List<UserParticipation> participants;
    private final ParticipantClickListener listener;

    public interface ParticipantClickListener {
        void onContactClick(UserModel user);
        void onParticipantClick(UserModel user);
    }

    public TopParticipantsAdapter(List<UserParticipation> participants, ParticipantClickListener listener) {
        this.participants = participants;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.top_participation, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        UserParticipation participant = participants.get(position);
        holder.bind(participant);

//        holder.btnContact.setOnClickListener(v -> {
//            listener.onContactClick(participant.getUser());
//        });

        holder.itemView.setOnClickListener(v -> {
            listener.onParticipantClick(participant.getUser());
        });
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private final ImageView participantImage;
        private final TextView participantName;
        private final TextView participationCount;
        private final TextView lastParticipation;
        private final TextView participantSkills;
        private final Button btnContact;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            participantImage = itemView.findViewById(R.id.participantImage);
            participantName = itemView.findViewById(R.id.participantName);
            participationCount = itemView.findViewById(R.id.participationCount);
            lastParticipation = itemView.findViewById(R.id.lastParticipation);
            participantSkills = itemView.findViewById(R.id.participantSkills);
            btnContact = itemView.findViewById(R.id.btnContact);
        }

        public void bind(UserParticipation participant) {
            UserModel user = participant.getUser();

            // Load profile image
//            Glide.with(itemView.getContext())
//                    .load(user.getProfileImageUrl())
//                    .placeholder(R.drawable.ic_profile_placeholder)
//                    .into(participantImage);

            participantName.setText(user.getFullName());
            participationCount.setText(participant.getParticipationCount() + " events");

            // Format last participation date
//            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
//            String lastDate = "Last joined: " + dateFormat.format(participant.getLastParticipationDate());
//            lastParticipation.setText(lastDate);
//            lastParticipation.setText("Last joined: " +
//                    DateUtils.formatTimestamp(participant.getLastParticipationDate()));
            List<String> skillsList = user.getSkills();

            if (skillsList != null && !skillsList.isEmpty()) {
                String skills = "Skills: " + String.join(", ", skillsList);
                participantSkills.setText(skills);
            } else {
                participantSkills.setText("Skills: Not specified");
            }
        }
    }
}