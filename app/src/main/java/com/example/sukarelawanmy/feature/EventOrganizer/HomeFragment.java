package com.example.sukarelawanmy.feature.EventOrganizer;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sukarelawanmy.R;


import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sukarelawanmy.databinding.FragmentHomeBinding;
import com.example.sukarelawanmy.model.EventNGOModel;
import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.example.sukarelawanmy.model.EventParticipation;
import com.example.sukarelawanmy.model.PendingRequestModel;
import com.example.sukarelawanmy.model.UserModel;
import com.example.sukarelawanmy.model.UserParticipation;
import com.example.sukarelawanmy.model.Volunteer;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private EventAdapter eventAdapter;
    private final List<EventNGOModel> eventList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private NavController navController;
    private ShareViewModel userViewModel;
    ProgressDialog progress;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(HomeFragment.this);
        userViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        progress = new ProgressDialog(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userViewModel.getUser().observe(getViewLifecycleOwner(), userModel -> {
            if (userModel != null) {
                Log.d("TAG", "onViewCreated: " + userModel);
                binding.welcomeMessage.setText("Welcome back,");
                binding.userName.setText(userModel.getFullName() != null ? userModel.getFullName() : "Volunteer");
                binding.memberSince.setText("Organizer");
            }
        });
        binding.title.setOnClickListener(v -> {
         //   navConntroller.navigate(R.id.action_homeFragment_to_profileMangmentOrgFragment);
        });
        setupRecyclerView();
        setupFAB();
        loadDataFromFireStore();
    }
    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(eventList, event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getId());
            navController.navigate(R.id.action_homeFragment_to_eventDetailsFragment, bundle);
        });

        binding.recyler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyler.setAdapter(eventAdapter);
    }
    private void showEmptyState(boolean show) {
        if (binding != null) {
            binding.recyler.setVisibility(show ? View.GONE : View.VISIBLE);

            binding.emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void setupFAB() {
        binding.fab.setOnClickListener(v -> {
            navController.navigate(R.id.action_homeFragment_to_createEventFragment);
        });
    }

    private void loadDataFromFireStore() {
        String ngoId = FirebaseAuth.getInstance().getCurrentUser().getUid();

// 1. Get total number of events created by this NGO
        db.collection("events")
                .whereEqualTo("ngoId", ngoId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalEvents = querySnapshot.size();
                    binding.totalEvents.setText(String.valueOf(totalEvents));
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error counting events", e);
                    binding.totalEvents.setText("0");
                });

// 2. Get total number of volunteers across all events
        db.collection("event_participants")
                .whereEqualTo("ngoId", ngoId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalVolunteers = querySnapshot.size();
                    binding.totalVolunteers.setText(String.valueOf(totalVolunteers));

                    // For active volunteers (approved status)
                    long activeVolunteers = querySnapshot.getDocuments().stream()
                            .filter(doc -> "approved".equals(doc.getString("status")))
                            .count();
                    //      binding.activeVolunteers.setText(String.valueOf(activeVolunteers));

                    // For new volunteers (joined in last 30 days)
                    long newVolunteers = querySnapshot.getDocuments().stream()
                            .filter(doc -> {
                                Timestamp joinedAt = doc.getTimestamp("joinedAt");
                                return joinedAt != null &&
                                        joinedAt.toDate().after(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)));
                            })
                            .count();
                    binding.newVolunteers.setText(String.valueOf(newVolunteers));
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error counting volunteers", e);
                    binding.totalVolunteers.setText("0");
                    //  binding.activeVolunteers.setText("0");
                    binding.newVolunteers.setText("0");
                });

// 3. Get latest 3 events (your existing code)
        // First get all events created by this NGO
        db.collection("events")
                .whereEqualTo("ngoId", ngoId)
                .get()
                .addOnSuccessListener(eventsQuerySnapshot -> {
                    if (eventsQuerySnapshot.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    // Create a map of event IDs to participant counts
                    Map<String, Integer> eventParticipantCounts = new HashMap<>();
                    List<Task<QuerySnapshot>> participantCountTasks = new ArrayList<>();

                    // For each event, count participants
                    for (DocumentSnapshot eventDoc : eventsQuerySnapshot.getDocuments()) {
                        String eventId = eventDoc.getId();
                        Task<QuerySnapshot> countTask = db.collection("event_participants")
                                .whereEqualTo("eventId", eventId)
                                .whereEqualTo("status", "approved") // Only count approved participants
                                .get()
                                .addOnSuccessListener(participantsSnapshot -> {
                                    eventParticipantCounts.put(eventId, participantsSnapshot.size());
                                });
                        participantCountTasks.add(countTask);
                    }

                    // When all participant counts are done
                    Tasks.whenAllSuccess(participantCountTasks)
                            .addOnSuccessListener(results -> {
                                // Sort events by participant count (descending)
                                List<DocumentSnapshot> sortedEvents = eventsQuerySnapshot.getDocuments().stream()
                                        .sorted((e1, e2) -> {
                                            int count1 = eventParticipantCounts.getOrDefault(e1.getId(), 0);
                                            int count2 = eventParticipantCounts.getOrDefault(e2.getId(), 0);
                                            return Integer.compare(count2, count1); // Descending order
                                        })
                                        .limit(3) // Take top 3
                                        .collect(Collectors.toList());

                                // Process the top 3 events
                                eventList.clear();
                                for (DocumentSnapshot document : sortedEvents) {
                                    try {
                                        EventNGOModel event = document.toObject(EventNGOModel.class);
                                        if (event != null) {
                                            event.setId(document.getId());
                                            event.setParticipantCount(eventParticipantCounts.get(document.getId()));
                                            eventList.add(event);
                                        }
                                    } catch (Exception e) {
                                        Log.e("Firestore", "Error parsing event document: " + document.getId(), e);
                                    }
                                }
                                eventAdapter.notifyDataSetChanged();
                                showEmptyState(eventList.isEmpty());
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firestore", "Error counting participants", e);
                                showEmptyState(true);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error loading events", e);
                    Toast.makeText(requireContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
        findMostActiveUsers();

    }
    private void findMostActiveUsers() {
        String ngoId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        showProgressDialog("Finding top participants...");

        // First get all event IDs for this NGO
        db.collection("events")
                .whereEqualTo("ngoId", ngoId)
                .get()
                .addOnSuccessListener(eventsSnapshot -> {
                    if (eventsSnapshot.isEmpty()) {
                        dismissProgressDialog();
                        binding.emptyState2.setVisibility(View.VISIBLE);
                        binding.topRecyler.setVisibility(View.GONE);
                        showToast("No events found");
                        return;
                    }

                    List<String> eventIds = new ArrayList<>();
                    for (DocumentSnapshot eventDoc : eventsSnapshot) {
                        eventIds.add(eventDoc.getId());
                    }

                    // Now count participations per user
                    db.collection("event_participants")
                            .whereIn("eventId", eventIds)
                            .whereEqualTo("status", "approved") // Only count approved participations
                            .get()
                            .addOnSuccessListener(participantsSnapshot -> {
                                Map<String, Integer> userParticipationCounts = new HashMap<>();
                                Map<String, DocumentSnapshot> userDocsMap = new HashMap<>();
                                List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

                                // Count participations per user
                                for (DocumentSnapshot doc : participantsSnapshot) {
                                    String userId = doc.getString("userId");
                                    userParticipationCounts.put(userId,
                                            userParticipationCounts.getOrDefault(userId, 0) + 1);
                                }

                                // Sort users by participation count (descending)
                                List<Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(
                                        userParticipationCounts.entrySet());
                                sortedUsers.sort((a, b) -> b.getValue().compareTo(a.getValue()));

                                // Get top 5 most active users
                                int limit = Math.min(5, sortedUsers.size());
                                List<String> topUserIds = new ArrayList<>();
                                for (int i = 0; i < limit; i++) {
                                    topUserIds.add(sortedUsers.get(i).getKey());
                                }

                                if (topUserIds.isEmpty()) {
                                    dismissProgressDialog();
                                    binding.emptyState2.setVisibility(View.VISIBLE);
                                    binding.topRecyler.setVisibility(View.GONE);
                                    showToast("No participants found");
                                    return;
                                }

                                // Fetch user details for top participants
                                for (String userId : topUserIds) {
                                    userTasks.add(db.collection("users").document(userId).get());
                                }

                                Tasks.whenAllSuccess(userTasks).addOnSuccessListener(userDocuments -> {
                                    List<UserParticipation> topParticipants = new ArrayList<>();

                                    for (Object result : userDocuments) {
                                        DocumentSnapshot userDoc = (DocumentSnapshot) result;
                                        String userId = userDoc.getId();
                                        UserModel user = userDoc.toObject(UserModel.class);
                                        if (user != null) {
                                            UserParticipation participant = new UserParticipation();
                                            participant.setUser(user);
                                            participant.setParticipationCount(userParticipationCounts.get(userId));
                                            topParticipants.add(participant);
                                        }
                                    }

                                    // Sort again in case user fetches completed out of order
                                    topParticipants.sort((a, b) ->
                                            Integer.compare(b.getParticipationCount(), a.getParticipationCount()));

                                    dismissProgressDialog();
                                    displayTopParticipants(topParticipants);
                                });
                            })
                            .addOnFailureListener(e -> {
                                dismissProgressDialog();
                                binding.emptyState2.setVisibility(View.VISIBLE);
                                binding.topRecyler.setVisibility(View.GONE);
                                Log.e("MostActiveUsers", "Error counting participations", e);
                                showToast("Failed to analyze participation: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Log.e("MostActiveUsers", "Error loading events", e);
                    showToast("Failed to load events: " + e.getMessage());
                });
    }

    private void displayTopParticipants(List<UserParticipation> topParticipants) {
        // Initialize adapter


        TopParticipantsAdapter adapter = new TopParticipantsAdapter(topParticipants, new TopParticipantsAdapter.ParticipantClickListener() {
            @Override
            public void onContactClick(UserModel user) {
                // Handle contact button click

            }

            @Override
            public void onParticipantClick(UserModel user) {
                // Handle participant item click
              //  openParticipantDetails(user);
            }
        });

        binding.topRecyler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.topRecyler.setAdapter(adapter);
        if (topParticipants.isEmpty()){
            binding.emptyState2.setVisibility(View.VISIBLE);
            binding.topRecyler.setVisibility(View.GONE);
        }else {
            binding.emptyState2.setVisibility(View.GONE);
            binding.topRecyler.setVisibility(View.VISIBLE);
        }
    }




    private void showToast(String volunteerApprovedSuccessfully) {
        Toast.makeText(requireContext(), volunteerApprovedSuccessfully, Toast.LENGTH_SHORT).show();
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
}