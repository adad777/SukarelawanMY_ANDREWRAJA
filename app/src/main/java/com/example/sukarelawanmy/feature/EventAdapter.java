package com.example.sukarelawanmy.feature;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.EventItemBinding;
import com.example.sukarelawanmy.model.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<Event> events;

    public EventAdapter(List<Event> events) {
        this.events = events;
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
        holder.binding.eventDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(event.getDate()));
        Glide.with(holder.itemView.getContext())
                .load("https://picsum.photos/200/300") // sample image URL
                .placeholder(R.drawable.placeholder_image) // optional
                .error(R.drawable.placeholder_image) // optional
                .into(holder.binding.image);
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            // Navigate to event details
//            Navigation.findNavController(v).navigate(
//                    DashboardFragmentDirections.actionDashboardFragmentToEventDetailFragment(event.getId())
//            );
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