package com.example.sukarelawanmy.feature.myevents;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentMyEventsBinding;
import com.example.sukarelawanmy.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MyEventsFragment extends Fragment {

    private FragmentMyEventsBinding binding;
    private EventsAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private NavController navController;
    private CollectionReference eventsRef;
    private CollectionReference userEventsRef;
    private ListenerRegistration eventsListener;
    ProgressDialog progress;
    private List<Event> joinedEventsList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMyEventsBinding.inflate(inflater, container, false);
        progress = new ProgressDialog(requireContext());
        navController = NavHostFragment.findNavController(MyEventsFragment.this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        loadJoinedEventsWithDetails();

    }

    private void setupRecyclerView() {
        adapter = new EventsAdapter(events, new EventsAdapter.EventClickListener() {
            @Override
            public void onEventClick(Event event) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                navController.navigate(R.id.action_myEventsFragment_to_eventDetailsFragment, bundle);
            }

            @Override
            public void onJoinClick(Event event, boolean isJoining) {
                if (isJoining) {
                    joinEvent(event);
                } else {
                    showUnJoinConfirmation(event);
                }
            }
        });

        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.eventsRecyclerView.setAdapter(adapter);
    }

    private void loadJoinedEventsWithDetails() {
        showProgressDialog("Loading your events...");

        String userId = auth.getUid();
        if (userId == null) {
            dismissProgressDialog();
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("event_participants")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e("MyEvents", "Failed to get joined events", task.getException());
                        dismissProgressDialog();
                        Toast.makeText(requireContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> eventIds = new ArrayList<>();
                    for (DocumentSnapshot doc : task.getResult()) {
                        String eventId = doc.getString("eventId");
                        if (eventId != null) {
                            eventIds.add(eventId);
                        }
                    }

                    if (eventIds.isEmpty()) {
                        dismissProgressDialog();
                        showEmptyState(true);
                        return;
                    }

                    // Now fetch full event data
                    db.collection("events")
                            .whereIn("eventId", eventIds)
                            .get()
                            .addOnCompleteListener(eventTask -> {
                                joinedEventsList.clear();
                                if (eventTask.isSuccessful() && eventTask.getResult() != null) {
                                    for (DocumentSnapshot doc : eventTask.getResult()) {
                                        Event event = doc.toObject(Event.class);
                                        joinedEventsList.add(event);
                                    }
                                    adapter.updateJoinedEvents(joinedEventsList);
                                    showEmptyState(joinedEventsList.isEmpty());
                                } else {
                                    Log.e("MyEvents", "Failed to load event details", eventTask.getException());
                                    showEmptyState(true);
                                }
                                dismissProgressDialog();
                            });
                });
    }
    private void showEmptyState(boolean show) {
        binding.eventsRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void navigateToEventDetail(Event event) {

    }

    private void joinEvent(Event event) {
        userEventsRef.document(event.getEventId())
                .set(new HashMap<String, String>() {{
                    put("joinedAt", new Date().toString());
                }})
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Joined: " + event.getTitle(), Toast.LENGTH_SHORT).show();
                    adapter.addJoinedEvent(event.getEventId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to join event", Toast.LENGTH_SHORT).show();
                });
    }

    private void showUnJoinConfirmation(Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Unjoin")
                .setMessage("Are you sure you want to unjoin " + event.getTitle() + "?")
                .setPositiveButton("Unjoin", (dialog, which) -> {
                    unjoinEvent(event);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showProgressDialog(String message) {
        progress.setMessage(message);
        progress.setCancelable(false);
        progress.show();
    }

    private void dismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }
    private void unjoinEvent(Event event) {
        userEventsRef.document(event.getEventId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Unjoined: " + event.getTitle(), Toast.LENGTH_SHORT).show();
                    adapter.removeJoinedEvent(event.getEventId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to unjoin event", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventsListener != null) {
            eventsListener.remove();
        }
        binding = null;
    }
}