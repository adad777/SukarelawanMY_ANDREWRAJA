package com.example.sukarelawanmy.feature.allevents;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentAllEventsBinding;
import com.example.sukarelawanmy.databinding.ItemEventBinding;
import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.example.sukarelawanmy.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllEventsFragment extends Fragment {

    private FragmentAllEventsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EventsAdapter eventsAdapter;
    private String currentUserType = "Volunteer";
    private ProgressDialog progress;
    private NavController navController;
    private ShareViewModel sharedViewModel;
    private List<Event> eventArrayList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAllEventsBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        progress = new ProgressDialog(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        checkUserTypeAndLoadEvents();
        binding.searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void filter(String string) {
        ArrayList<Event> filteredList = new ArrayList<>();

        for (Event event : eventArrayList) {
            if (event.getTitle().toLowerCase().contains(string.toLowerCase())) {
                filteredList.add(event);
            }
        }
        eventsAdapter.setFilteredEvents(filteredList);
    }

    private void setupRecyclerView() {
        eventsAdapter = new EventsAdapter(new ArrayList<>(), currentUserType, new EventsAdapter.EventClickListener() {
            @Override
            public void onEventClick(Event event) {
                navigateToEventDetail(event);
            }

            @Override
            public void onJoinClick(Event event, boolean isJoining) {
                if (isJoining) {
                    joinEvent(event);
                } else {
                    showUnjoinConfirmation(event);
                }
            }

            @Override
            public void onEditClick(Event event) {
                navigateToEditEvent(event);
            }

            @Override
            public void onViewParticipantsClick(Event event) {
                navigateToParticipants(event);
            }
        });

        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.eventsRecyclerView.setAdapter(eventsAdapter);
    }



    private void checkUserTypeAndLoadEvents() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showToast("Please sign in");
            return;
        }

        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserType = documentSnapshot.getString("role") != null ?
                                documentSnapshot.getString("role") : "volunteer";
                        eventsAdapter.setUserType(currentUserType);
                        loadEvents();
                    }
                })
                .addOnFailureListener(e -> showToast("Failed to check user type"));
    }

    private void loadEvents() {
        showProgressDialog();

        db.collection("events")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Event event = new Event();
                            event.setEventId(document.getId());
                            event.setTitle(document.getString("title"));
                            event.setLocation(document.getString("location"));
                            event.setDate(document.getString("date"));
                            event.setDescription(document.getString("description"));
                            event.setImageUrl(document.getString("imageUrl"));
                            event.setMaxParticipants(document.getLong("maxParticipants") != null ? document.getLong("maxParticipants").intValue() : 0);
                            event.setCurrentParticipants(document.getLong("currentParticipants") != null ? document.getLong("currentParticipants").intValue() : 0);
                            event.setRequiresApproval(Boolean.TRUE.equals(document.getBoolean("requiresApproval")));
                            event.setNgoId(document.getString("ngoId"));
                            event.setTime(document.getString("time"));
                            event.setOrganizerName(document.getString("organizerName"));
                            event.setEventCategory(document.getString("eventCategory"));
                            Long eventMinutesLong = document.getLong("totalMinutes");
                            int eventMinutes = eventMinutesLong != null ? eventMinutesLong.intValue() : 0;
                            event.setTotalMinutes(eventMinutes);

                            Object reqObj = document.get("requirement");
                            if (reqObj instanceof List) {
                                event.setRequirement(new ArrayList<>((List<String>) reqObj));
                            } else if (reqObj instanceof String) {
                                ArrayList<String> tempList = new ArrayList<>();
                                tempList.add((String) reqObj);
                                event.setRequirement(tempList);
                            } else {
                                event.setRequirement(new ArrayList<>());
                            }

                            events.add(event);
                        } catch (Exception e) {
                            Log.e("EventParsing", "Failed to parse event: " + e.getMessage());
                        }

                    }

                    if (currentUserType.equals("Volunteer")) {
                        checkJoinedEvents(events);
                    } else {
                        updateAdapter(events);
                    }
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    showToast("Failed to load events");
                });
    }

    private void searchEvents(String query) {
        showProgressDialog();

        db.collection("events")
                .orderBy("title")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Event> events = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Event event = document.toObject(Event.class);
                        event.setEventId(document.getId());
                        events.add(event);
                    }

                    if (currentUserType.equals("Volunteer")) {
                        checkJoinedEvents(events);
                    } else {
                        updateAdapter(events);
                    }
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    showToast("Search failed");
                });
    }

    private void checkJoinedEvents(List<Event> events) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            updateAdapter(events);
            return;
        }

        db.collection("event_participants")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> joinedEventIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        joinedEventIds.add(document.getString("eventId"));
                    }

                    for (Event event : events) {
                        event.setJoined(joinedEventIds.contains(event.getEventId()));
                    }

                    updateAdapter(events);
                })
                .addOnFailureListener(e -> updateAdapter(events));
    }

    private void updateAdapter(List<Event> events) {
        try {

            eventsAdapter.setEvents(events);
            eventArrayList.clear();
            eventArrayList = events;

            dismissProgressDialog();

            if (events.isEmpty()) {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.eventsRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyState.setVisibility(View.GONE);
                binding.eventsRecyclerView.setVisibility(View.VISIBLE);
            }
        }catch (Exception e){}

    }

    private void joinEvent(Event event) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        showProgressDialog("Joining event...");
        createAutomaticParticipation(event, user.getUid());
    }

    private void showUnjoinConfirmation(Event event) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cancel Participation")
                .setTitle("Cancel Participation")
                .setMessage("Are you sure you want to cancel your participation?")
                .setPositiveButton("Yes", (dialog, which) -> cancelParticipation(event))
                .setNegativeButton("No", null)
                .show();
    }
    private void cancelParticipation(Event event) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Please sign in to cancel participation");
            return;
        }

        String userId = currentUser.getUid();
        String eventId = event.getEventId();

        // Step 1: Find the participant document
        db.collection("event_participants")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot participationDoc = querySnapshot.getDocuments().get(0);
                        String participationId = participationDoc.getId();

                        // Step 2: Delete the document and update counts
                        WriteBatch batch = db.batch();

                        // Delete participation
                        DocumentReference participantRef = db.collection("event_participants").document(participationId);
                        batch.delete(participantRef);

                        // Decrease event participant count
                        DocumentReference eventRef = db.collection("events").document(eventId);
                        batch.update(eventRef, "currentParticipants", FieldValue.increment(-1));

                        // Decrease user's attended event count
                        DocumentReference userRef = db.collection("users").document(userId);
                        batch.update(userRef, "eventsAttended", FieldValue.increment(-1));
                        batch.update(userRef, "totalMinutesVolunteered", FieldValue.increment(-event.getTotalMinutes()));

                        // Commit batch
                        showProgressDialog("Cancelling participation...");
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    dismissProgressDialog();
                                    showToast("Participation cancelled successfully");
                                    event.setJoined(false);
                                    eventsAdapter.notifyDataSetChanged();

                                })
                                .addOnFailureListener(e -> {
                                    dismissProgressDialog();
                                    Log.e("CancelEvent", "Batch cancel failed", e);
                                    showToast("Failed to cancel participation");
                                });
                    } else {
                        showToast("You are not a participant in this event");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CancelEvent", "Failed to check participation", e);
                    showToast("Error checking your participation status");
                });
    }

    private void navigateToEventDetail(Event event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getEventId());
        navController.navigate(R.id.action_allEventsFragment_to_eventDetailsFragment, args);
    }

    private void navigateToEditEvent(Event event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getEventId());
        navController.navigate(R.id.action_allEventsFragment_to_createEventFragment, args);
    }

    private void navigateToParticipants(Event event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getEventId());
        navController.navigate(R.id.action_allEventsFragment_to_participationFragment, args);
    }

    private void showProgressDialog() {
        showProgressDialog("Loading...");
    }

    private void showProgressDialog(String message) {
        progress.setMessage("Loading...");
        progress.setCancelable(false);
        progress.show();
    }

    private void dismissProgressDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.EventViewHolder> {

        private List<Event> events;
        private String userType;
        private final EventClickListener listener;

        public interface EventClickListener {
            void onEventClick(Event event);
            void onJoinClick(Event event, boolean isJoining);
            void onEditClick(Event event);
            void onViewParticipantsClick(Event event);
        }

        public EventsAdapter(List<Event> events, String userType, EventClickListener listener) {
            this.events = events;
            this.userType = userType;
            this.listener = listener;
        }

        public void setEvents(List<Event> events) {
            this.events = events;
            notifyDataSetChanged();
        }
        public void setFilteredEvents(List<Event> filteredEvents) {
            this.events = filteredEvents;
            notifyDataSetChanged();
        }
        public List<Event> getEvents() {
            return events;
        }

        public void setUserType(String userType) {
            this.userType = userType;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemEventBinding binding = ItemEventBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new EventViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            Event event = events.get(position);
            holder.bind(event, userType);
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class EventViewHolder extends RecyclerView.ViewHolder {
            private final ItemEventBinding binding;

            public EventViewHolder(@NonNull ItemEventBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Event event, String userType) {
                binding.txtEventTitle.setText(event.getTitle());
                binding.txtEventDate.setText(event.getDate());
                binding.txtEventTime.setText(event.getTime());
                binding.chipEventCategory.setText(event.getEventCategory());
//
//                Glide.with(itemView.getContext())
//                        .load(event.getImageUrl())
//                        .placeholder(R.drawable.event_placeholder)
//                        .into(binding.imgEventBanner);

                if (userType.equals("Volunteer")) {
                    binding.layoutOrganizerOptions.setVisibility(View.GONE);
                    binding.btnJoinEvent.setVisibility(View.VISIBLE);

                    if (event.isJoined()) {
                        binding.btnJoinEvent.setText("Cancel Participation");
                        binding.btnJoinEvent.setBackgroundTintList(
                                ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.orange)));
                    } else {
                        binding.btnJoinEvent.setText("Join Event");
                        binding.btnJoinEvent.setBackgroundTintList(
                                ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), R.color.orange)));
                    }

                    binding.btnJoinEvent.setOnClickListener(v -> listener.onJoinClick(event, !event.isJoined()));
                } else {
                    binding.btnJoinEvent.setVisibility(View.GONE);
                    binding.layoutOrganizerOptions.setVisibility(View.VISIBLE);
                    binding.btnEditEvent.setOnClickListener(v -> listener.onEditClick(event));
                    binding.btnViewParticipants.setOnClickListener(v -> listener.onViewParticipantsClick(event));
                }

                itemView.setOnClickListener(v -> listener.onEventClick(event));
            }
        }
    }

    private void createAutomaticParticipation(Event event, String userId) {
        showProgressDialog("Joining event...");

        String participantId = db.collection("event_participants").document().getId();
        DocumentReference participantRef = db.collection("event_participants").document(participantId);

        Task<DocumentSnapshot> userTask = db.collection("users")
                .document(sharedViewModel.getUser().getValue().getUid())
                .get();

        Task<DocumentSnapshot> organizerTask = db.collection("users")
                .document(event.getNgoId())
                .get();

        Tasks.whenAllSuccess(userTask, organizerTask)
                .addOnSuccessListener(results -> {
                    DocumentSnapshot userDoc = (DocumentSnapshot) results.get(0);
                    DocumentSnapshot organizerDoc = (DocumentSnapshot) results.get(1);

                    Map<String, Object> participation = new HashMap<>();

                    // Event details
                    participation.put("eventId", event.getEventId());
                    participation.put("eventName", event.getTitle());
                    participation.put("eventDate", event.getDate());
                    participation.put("eventLocation", event.getLocation());

                    // Participant details
                    participation.put("userId", userId);
                    participation.put("userName", userDoc.getString("fullName"));
                    participation.put("userEmail", userDoc.getString("email"));
                    participation.put("userPhone", userDoc.getString("phone"));
                    participation.put("userImageUrl", userDoc.getString("profileImageUrl"));

                    // Organizer details
                    participation.put("organizerId", event.getNgoId());
                    participation.put("organizerName", organizerDoc.getString("fullName"));
                    participation.put("organizerContact", organizerDoc.getString("phone"));

                    // Status and timestamps
                    participation.put("status", "approved");
                    participation.put("joinedAt", FieldValue.serverTimestamp());
                    participation.put("approvedAt", FieldValue.serverTimestamp());
                    participation.put("ngoId", event.getNgoId());

                    // Create participation document
                    participantRef.set(participation)
                            .addOnSuccessListener(aVoid -> {
                                event.setJoined(true);
                                eventsAdapter.notifyDataSetChanged();
                                updateParticipantCount(event, userId);
                            })
                            .addOnFailureListener(e -> {
                                dismissProgressDialog();
                                handleJoinError(e);
                            });

                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    handleJoinError(e); // Handle failure of loading user or organizer
                });
    }

    private void updateParticipantCount(Event event, String userId) {
        WriteBatch batch = db.batch();

        DocumentReference eventRef = db.collection("events").document(event.getEventId());
        batch.update(eventRef, "currentParticipants", FieldValue.increment(1));

        DocumentReference userRef = db.collection("users").document(userId);
        batch.update(userRef, "eventsAttended", FieldValue.increment(1));
        batch.update(userRef, "totalMinutesVolunteered", FieldValue.increment(event.getTotalMinutes()));


        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    showToast("Successfully joined the event!");

                })
                .addOnFailureListener(e -> {
                    Log.e("JoinEvent", "Batch update failed", e);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    handleBatchFailureFallback(event, userId);
                });
    }

    private void handleBatchFailureFallback(Event event, String userId) {
        DocumentReference eventRef = db.collection("events").document(event.getEventId());
        DocumentReference userRef = db.collection("users").document(userId);

        eventRef.update("currentParticipants", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    userRef.update("eventsAttended", FieldValue.increment(1))
                            .addOnSuccessListener(aVoid1 -> {
                                dismissProgressDialog();
                                showToast("Joined event (fallback success)!");

                            })
                            .addOnFailureListener(e -> {
                                Log.e("JoinEvent", "User update failed", e);
                                dismissProgressDialog();
                                showToast("Joined event but stats not updated");

                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("JoinEvent", "Event update failed", e);
                    dismissProgressDialog();
                    showToast("Failed to update event participants");
                });
    }

    private void handleExistingParticipation(DocumentSnapshot participationDoc) {
        String status = participationDoc.getString("status");
        if ("approved".equals(status)) {
            showToast("You're already participating in this event");
        } else if ("pending".equals(status)) {
            showToast("Your request is still pending");
        } else if ("rejected".equals(status)) {
            showToast("Your previous request was rejected");
        }
    }

    private void handleJoinError(Exception e) {
        Log.e("JoinEvent", "Error joining event", e);
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreEx = (FirebaseFirestoreException) e;
            if (firestoreEx.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                showToast("Permission denied. Please try again.");
            } else {
                showToast("Failed to join event: " + e.getMessage());
            }
        } else {
            showToast("Failed to join event");
        }
    }

}