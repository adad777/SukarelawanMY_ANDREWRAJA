package com.example.sukarelawanmy.feature.EventOrganizer;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.EventItemNgoBinding;
import com.example.sukarelawanmy.model.Event;
import com.example.sukarelawanmy.model.EventNGOModel;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<EventNGOModel> events;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EventNGOModel event);
    }

    public EventAdapter(List<EventNGOModel> events, OnItemClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        EventItemNgoBinding binding = EventItemNgoBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(events.get(position));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final EventItemNgoBinding binding;

        EventViewHolder(EventItemNgoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(EventNGOModel event) {
            binding.eventTitle.setText(event.getTitle());
            binding.volunteerCount.setText(event.getVolunteerCount() + " volunteers");

//            Glide.with(binding.getRoot().getContext())
//                    .load(event.getImageUrl())
//                    .placeholder(R.drawable.ic_default_event)
//                    .into(binding.eventImage);

            binding.getRoot().setOnClickListener(v -> listener.onItemClick(event));
        }
    }
}