package com.example.sukarelawanmy.feature.myevents;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.MyEventItemBinding;
import com.example.sukarelawanmy.model.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

    private List<Event> eventList = new ArrayList<>();
    private EventClickListener listener;
    private Map<String, Boolean> joinedEventMap = new HashMap<>();

    public interface EventClickListener {
        void onEventClick(Event event);
        void onJoinClick(Event event, boolean isJoining);
    }

    public EventsAdapter(List<Event> eventList, EventClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    public void updateJoinedEvents(List<Event> joinedEvents) {
        eventList.clear();
        eventList.addAll(joinedEvents);
        notifyDataSetChanged();
    }

    public void addJoinedEvent(String eventId) {
        joinedEventMap.put(eventId, true);
        notifyDataSetChanged();
    }

    public void removeJoinedEvent(String eventId) {
        joinedEventMap.remove(eventId);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MyEventItemBinding binding = MyEventItemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        boolean isJoined = isUserJoined(event);

        holder.bind(event, isJoined);
        holder.binding.getRoot().setOnClickListener(v -> listener.onEventClick(event));
        holder.binding.btnJoin.setOnClickListener(v -> listener.onJoinClick(event, !isJoined));
    }

    private boolean isUserJoined(Event event) {
        return joinedEventMap.containsKey(event.getEventId());
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final MyEventItemBinding binding;

        public EventViewHolder(@NonNull MyEventItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Event event, boolean isJoined) {
            binding.eventTitle.setText(event.getTitle());
            binding.eventDate.setText("Date: " + event.getDate());
            binding.eventTime.setText("Time: " + event.getDate());
            binding.eventOrganizer.setText("Organized by: " + event.getOrganizerName());

            binding.btnJoin.setText(isJoined ? "UnJoin" : "Join");
            binding.btnJoin.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(),
                            isJoined ? R.color.orange : R.color.orange)
            ));
        }
    }
}