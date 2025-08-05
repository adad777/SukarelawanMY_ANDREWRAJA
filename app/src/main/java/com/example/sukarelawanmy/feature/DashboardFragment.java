package com.example.sukarelawanmy.feature;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentDashboardBinding;
import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.example.sukarelawanmy.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseFirestore db;
    private EventAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();
    private ShareViewModel sharedViewModel;
    private NavController navController;
    private String currentUserId;
    ProgressDialog progress;
    BottomNavigationView bottomNavigationView;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize ViewBinding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(DashboardFragment.this);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        progress = new ProgressDialog(requireContext());


        // Set default selected item (optional)

        return binding.getRoot();
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
              //  binding.welcomeText.setText("Welcome, " + user.getFullName());
            }
        });
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        loadUserData();
        setupRecyclerView();
        loadEvents();
        loadUserStats();
        binding.seeAll.setOnClickListener(v->{
            navController.navigate(R.id.action_dashboardFragment_to_myEventsFragment);
                });
//        binding.profileImage.setOnClickListener(v->{
//            navController.navigate(R.id.action_dashboardFragment_to_profileMangmentOrgFragment);
//        });

    }

    private void setupRecyclerView() {
        Log.d("TAG", "setupRecyclerView: " + eventList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);

        eventAdapter = new EventAdapter(db,currentUserId,eventList,new EventAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Event event) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                navController.navigate(R.id.action_dashboardFragment_to_eventDetailsFragment, bundle);
            }

            @Override
            public void onJoinClicked(Event event) {
                joinEvent(event);
            }
        });

        binding.recycler.setLayoutManager(layoutManager);
        binding.recycler.setAdapter(eventAdapter);
    }

    private void joinEvent(Event event) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showToast("Please sign in to join events");
            return;
        }
        String userId = currentUser.getUid();
        String eventId = event.getEventId();

        // Check if already participating
        db.collection("event_participants")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        handleExistingParticipation(task.getResult().getDocuments().get(0));
                        return;
                    }

                    // Check event capacity
                    if (event.getMaxParticipants() > 0 &&
                            event.getCurrentParticipants() >= event.getMaxParticipants()) {
                        showToast("This event is already full");
                        return;
                    }

                    // Automatically approve volunteer
                    createAutomaticParticipation(event, userId);
                })
                .addOnFailureListener(e -> {
                    Log.e("JoinEvent", "Error checking participation", e);
                    showToast("Error checking event status");
                });
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
                         //   updateParticipantCount(event, userId);
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

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    showToast("Successfully joined the event!");
                    loadEvents(); // Refresh UI
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
                                loadEvents();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("JoinEvent", "User update failed", e);
                                dismissProgressDialog();
                                showToast("Joined event but stats not updated");
                                loadEvents();
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

    // Helper methods
    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }


    private void loadUserData() {
        sharedViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Set welcome message and name
                binding.welcomeMessage.setText("Welcome back,");
                binding.userName.setText(user.getFullName() != null ? user.getFullName() : "Volunteer");
                binding.memberSince.setText(user.getRole());
            }
        });
    }

    private void loadEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w("loadJoinedEvents", "User not logged in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("event_participants")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(participantSnapshots -> {
                    if (participantSnapshots.isEmpty()) {
                        eventList.clear();
                        eventAdapter.notifyDataSetChanged();
                        showEmptyState(true);
                        return;
                    }

                    List<String> joinedEventIds = new ArrayList<>();
                    for (DocumentSnapshot doc : participantSnapshots) {
                        String eventId = doc.getString("eventId");
                        if (eventId != null) {
                            joinedEventIds.add(eventId);
                        }
                    }

                    if (joinedEventIds.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    // Fetch events using whereIn (limit 10 items per query)
                    db.collection("events")
                            .whereIn("eventId", joinedEventIds)
                            .get()
                            .addOnSuccessListener(eventSnapshots -> {
                                eventList.clear();
                                for (DocumentSnapshot eventDoc : eventSnapshots) {
                                    Event event = eventDoc.toObject(Event.class);
                                    if (event != null) {
                                        event.setEventId(eventDoc.getId());
                                        eventList.add(event);
                                    }
                                }

                                eventAdapter.notifyDataSetChanged();
                                showEmptyState(eventList.isEmpty());
                            })
                            .addOnFailureListener(e -> {
                                Log.e("loadJoinedEvents", "Error fetching events", e);
                                showEmptyState(true);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("loadJoinedEvents", "Error fetching participations", e);
                    showEmptyState(true);
                });
    }


    private void showEmptyState(boolean show) {
        try {
            binding.recycler.setVisibility(show ? View.GONE : View.VISIBLE);
            binding.emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        } catch (Exception e) {

        }

    }


    private void loadUserStats() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Safely extract values or default to 0
                        long eventsAttended = documentSnapshot.getLong("eventsAttended") != null
                                ? documentSnapshot.getLong("eventsAttended")
                                : 0;

                        long totalMinutes = documentSnapshot.getLong("totalMinutesVolunteered") != null
                                ? documentSnapshot.getLong("totalMinutesVolunteered")
                                : 0;

// Convert to hours and minutes
                        long hours = totalMinutes / 60;
                        long minutes = totalMinutes % 60;

                        long impactScore = (eventsAttended * 10) + hours; // impactScore based on hours only

                        binding.textEventsCount.setText(String.valueOf(eventsAttended));
                        binding.textStatsCount.setText(hours + " h" ); // Example: 3h 45m
                        binding.impactScore.setText("Impact Score: " + impactScore);



                    } else {
                        setDefaultStats();
                    }
                })
                .addOnFailureListener(e -> {
                    setDefaultStats();
                    Log.d("loadUserStats", "Error loading stats", e);
                });
    }

    private void setDefaultStats() {
        binding.textEventsCount.setText("0");
        binding.textStatsCount.setText("0");
        binding.impactScore.setText("Impact Score: 0");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    // Event Adapter class

}