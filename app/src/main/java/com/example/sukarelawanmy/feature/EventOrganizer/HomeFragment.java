package com.example.sukarelawanmy.feature.EventOrganizer;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sukarelawanmy.R;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sukarelawanmy.databinding.FragmentHomeBinding;
import com.example.sukarelawanmy.model.Event;
import com.example.sukarelawanmy.model.EventNGOModel;
import com.example.sukarelawanmy.model.ShareViewModel;
import com.example.sukarelawanmy.model.Volunteer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private EventAdapter eventAdapter;
    private final List<EventNGOModel> eventList = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private NavController navController;
    private ShareViewModel userViewModel;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(HomeFragment.this);
        userViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userViewModel.getUser().observe(getViewLifecycleOwner(), userModel -> {
            if (userModel != null) {
                Log.d("TAG", "onViewCreated: "+userModel);
            //    binding.welcomeText.setText("Welcome, " + userModel.getFullName() + " (" + userModel.getRole() + ")");
            }
        });
        setupRecyclerView();
        setupFAB();
        loadDataFromFirestore();
    }

    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(eventList, event -> {
            // Handle item click - navigate to event details
            // You'll need to implement this navigation
        });

        binding.recyler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyler.setAdapter(eventAdapter);
    }

    private void setupFAB() {
        binding.fab.setOnClickListener(v -> {
            navController.navigate(R.id.action_homeFragment_to_createEventFragment);
        });
    }

    private void loadDataFromFirestore() {
        String ngoId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load Events
        db.collection("events")
                .whereEqualTo("ngoId", ngoId)
                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(requireContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    eventList.clear();
//                    for (QueryDocumentSnapshot document : snapshot.getDocuments()) {
//                        Event event = document.toObject(Event.class);
//                        event.setId(document.getId());
//                        eventList.add(event);
//                    }
                    eventAdapter.notifyDataSetChanged();
                });

        // Load Volunteer Stats
        db.collection("ngos").document(ngoId)
                .addSnapshotListener((document, error) -> {
                    if (document != null && document.exists()) {
                        Long totalVolunteers = document.getLong("totalVolunteers");
                        Long activeVolunteers = document.getLong("activeVolunteers");
                        Long newVolunteers = document.getLong("newVolunteers");

                        binding.totalVolunteers.setText(String.valueOf(totalVolunteers != null ? totalVolunteers : 0));
                        binding.activeVolunteers.setText(String.valueOf(activeVolunteers != null ? activeVolunteers : 0));
                        binding.newVolunteers.setText(String.valueOf(newVolunteers != null ? newVolunteers : 0));
                    } else {
                        // Optional: handle case when document doesn't exist
                        binding.totalVolunteers.setText("0");
                        binding.activeVolunteers.setText("0");
                        binding.newVolunteers.setText("0");
                    }
                });


        // Load Approval Queue
//        db.collection("volunteers")
//                .whereEqualTo("status", "pending")
//                .limit(1)
//                .addSnapshotListener((snapshot, error) -> {
//                    if (snapshot != null && !snapshot.isEmpty()) {
//                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) snapshot.getDocuments().get(0);
//                        Volunteer volunteer = document.toObject(Volunteer.class);
//
//                        binding.approvalCard.findViewById<TextView>(R.id.volunteerName).setText(
//                                volunteer.getName());
//                        binding.approvalCard.findViewById<TextView>(R.id.eventName).setText(
//                                "Application for " + volunteer.getAppliedEvent());
//
//                        binding.approvalCard.findViewById<Button>(R.id.approveButton).setOnClickListener(v -> {
//                            approveVolunteer(document.getId());
//                        });
//                    }
//                });
    }

    private void approveVolunteer(String volunteerId) {
        db.collection("volunteers").document(volunteerId)
                .update("status", "approved")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Volunteer approved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Approval failed", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}