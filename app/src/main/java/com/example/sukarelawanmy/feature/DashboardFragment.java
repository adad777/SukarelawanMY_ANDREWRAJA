package com.example.sukarelawanmy.feature;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.sukarelawanmy.databinding.FragmentDashboardBinding;
import com.example.sukarelawanmy.model.ShareViewModel;
import com.example.sukarelawanmy.util.DummyDataGenerator;
import com.example.sukarelawanmy.model.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private FirebaseFirestore db;
    private EventAdapter eventAdapter;
    private List<Event> eventList = new ArrayList<>();
    private ShareViewModel sharedViewModel;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Initialize ViewBinding
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
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
        // Setup RecyclerView
        setupRecyclerView();

        // Load data from Firestore
        loadEvents();
        loadUserStats();
    }

    private void setupRecyclerView() {
       // eventList.clear(); // dummy
      //  eventList = DummyDataGenerator.generateDummyEvents();
        Log.d("TAG", "setupRecyclerView: " + eventList);

// Create horizontal layout manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);

        eventAdapter = new EventAdapter(eventList);
        binding.recycler.setLayoutManager(layoutManager);
        binding.recycler.setAdapter(eventAdapter);

    }
    private void loadUserData() {
        sharedViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Set welcome message and name
                binding.welcomeMessage.setText("Welcome back,");
                binding.userName.setText(user.getFullName() != null ? user.getFullName() : "Volunteer");

                // Format and set join date
                if (user.getJoinDate() != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                    binding.memberSince.setText("Member since " + sdf.format(user.getJoinDate()));
                } else {
                    binding.memberSince.setText("");
                }
            }
        });
    }

    private void loadEvents() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w("loadEvents", "User not logged in");
            return;
        }

        String userId = currentUser.getUid();

        db.collection("events")
                .whereGreaterThanOrEqualTo("date", new Date())
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(5)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        eventList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Event event = document.toObject(Event.class);
                            event.setId(document.getId());
                            eventList.add(event);
                        }

                        eventAdapter.notifyDataSetChanged();

                        if (eventList.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                        }
                    } else {
                        Log.e("loadEvents", "Error loading events", task.getException());
                        showEmptyState(true);
                    }
                });
    }

    private void showEmptyState(boolean show) {
        binding.recycler.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
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

                        long hoursVolunteered = documentSnapshot.getLong("hoursVolunteered") != null
                                ? documentSnapshot.getLong("hoursVolunteered")
                                : 0;

                        long impactScore = documentSnapshot.getLong("impactScore") != null
                                ? documentSnapshot.getLong("impactScore")
                                : 0;

                        binding.textEventsCount.setText(String.valueOf(eventsAttended));
                        binding.textStatsCount.setText(String.valueOf(hoursVolunteered));
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Event Adapter class

}