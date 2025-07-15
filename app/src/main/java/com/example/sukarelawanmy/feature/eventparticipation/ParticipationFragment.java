package com.example.sukarelawanmy.feature.eventparticipation;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.sukarelawanmy.databinding.FragmentParticipationBinding;
import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.example.sukarelawanmy.model.EventParticipation;
import com.example.sukarelawanmy.model.UserModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.auth.User;

import java.util.HashMap;
import java.util.Map;

public class ParticipationFragment extends Fragment {
    private ProgressDialog progress;
    private NavController navController;
    private ShareViewModel sharedViewModel;
    private FragmentParticipationBinding binding;
    private String eventId;
    private ParticipantAdapter adapter;
    private final List<EventParticipation> participants = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentParticipationBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(this);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        progress = new ProgressDialog(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }

        setupRecyclerView();
        if (eventId != null) {
            fetchParticipantsWithUserDetails(eventId);
        } else {
            Toast.makeText(requireContext(), "Event ID not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecyclerView() {
        adapter = new ParticipantAdapter(participants, new ParticipantAdapter.ParticipantClickListener() {
            @Override
            public void onCancelParticipation(EventParticipation participation) {
                showCancelConfirmationDialog(participation);
            }

            @Override
            public void onParticipantClick(EventParticipation participation) {
                // Handle participant click if needed
            }
        });
        binding.eventsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.eventsRecyclerView.setAdapter(adapter);
    }

    private void fetchParticipantsWithUserDetails(String eventId) {
       showProgressDialog();

        db.collection("event_participants")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    participants.clear();
                    List<Task<DocumentSnapshot>> userTasks = new ArrayList<>();

                    // First pass: collect all user IDs and create fetch tasks
                    Map<String, EventParticipation> participationMap = new HashMap<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        EventParticipation participation = document.toObject(EventParticipation.class);
                        participation.setParticipationId(document.getId());
                        participationMap.put(participation.getUserId(), participation);
                        userTasks.add(db.collection("users").document(participation.getUserId()).get());
                    }

                    // Execute all user fetches in parallel
                    Tasks.whenAllSuccess(userTasks).addOnSuccessListener(results -> {
                        for (Object result : results) {
                            DocumentSnapshot userDoc = (DocumentSnapshot) result;
                            if (userDoc.exists()) {
                                UserModel user = userDoc.toObject(UserModel.class);
                                EventParticipation participation = participationMap.get(userDoc.getId());
                                if (participation != null && user != null) {
                                    // Combine user data with participation data
                                    participation.setUserName(user.getFullName());
                                    participation.setUserEmail(user.getEmail());
                                    participation.setUserPhone(user.getPhone());
                                    participation.setUserProfileImage(user.getProfileImageUrl());
                                    participation.setUserSkills(user.getSkills());
                                    participation.setUserAddress(user.getAddress());
                                    participants.add(participation);
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        dismissProgressDialog();

                        if (participants.isEmpty()) {
                            binding.emptyState.setVisibility(View.VISIBLE);
                        } else {
                            binding.emptyState.setVisibility(View.GONE);
                        }
                    }).addOnFailureListener(e -> {
                        Log.e("ParticipationFragment", "Error loading user details", e);
                      showProgressDialog();
                        Toast.makeText(requireContext(), "Failed to load participant details", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("ParticipationFragment", "Error loading participants", e);
              dismissProgressDialog();
                    Toast.makeText(requireContext(), "Failed to load participants", Toast.LENGTH_SHORT).show();
                });
    }

    private void showCancelConfirmationDialog(EventParticipation participation) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cancel Participation")
                .setMessage("Are you sure you want to remove " + participation.getUserName() + "'s participation?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    removeParticipation(participation);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void removeParticipation(EventParticipation participation) {
        if (participation.getParticipationId() == null) return;

        showProgressDialog();
        db.collection("event_participants").document(participation.getParticipationId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    participants.remove(participation);
                    adapter.notifyDataSetChanged();
                    dismissProgressDialog();
                    Toast.makeText(requireContext(), "Participation removed", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                   dismissProgressDialog();
                    Toast.makeText(requireContext(), "Failed to remove participation", Toast.LENGTH_SHORT).show();
                });
    }
    private void showProgressDialog() {
        progress.setMessage("Loading...");
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
        binding = null;
    }
}
