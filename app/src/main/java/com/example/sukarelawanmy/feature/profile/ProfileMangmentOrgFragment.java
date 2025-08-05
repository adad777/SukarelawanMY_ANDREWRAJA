package com.example.sukarelawanmy.feature.profile;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentProfileMangmentOrgBinding;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileMangmentOrgFragment extends Fragment {

    private FragmentProfileMangmentOrgBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private boolean isOrganizer = false;
    private List<String> originalSkills = new ArrayList<>();
    private Map<String, Object> originalData = new HashMap<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileMangmentOrgBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        checkUserType();
        setupSkillsInput();
        setupSaveButton();
        binding.logoutContainer.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void checkUserType() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userType = documentSnapshot.getString("role");
                        isOrganizer = "NGO".equals(userType);

                        // Hide skills section for organizers
                        if (isOrganizer) {
                            binding.skillTitle.setVisibility(View.GONE);
                            binding.skillsContainer.setVisibility(View.GONE);
                        }

                        // Load user data
                        loadUserData(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserData(DocumentSnapshot documentSnapshot) {
        // Store original data for comparison
        originalData.put("fullName", documentSnapshot.getString("fullName"));
        originalData.put("email", documentSnapshot.getString("email"));
        originalData.put("phone", documentSnapshot.getString("phone"));
        originalData.put("Address", documentSnapshot.getString("address"));

        // Set data to views
        // Set name in header card
        binding.textName.setText(documentSnapshot.getString("fullName"));
        if (isOrganizer) {
            binding.roleTitle.setText("Organizer");
        } else {
            binding.roleTitle.setText(documentSnapshot.getString("role"));
        }

// Set personal info fields
        binding.fullNameEditText.setText(documentSnapshot.getString("fullName"));
        binding.emailEditText.setText(documentSnapshot.getString("email"));
        binding.phoneEditText.setText(documentSnapshot.getString("phone"));
        binding.locationEdit.setText(documentSnapshot.getString("address"));

        // Load skills if not organizer
        if (!isOrganizer && documentSnapshot.contains("skills")) {
            originalSkills = (List<String>) documentSnapshot.get("skills");
            if (originalSkills != null) {
                populateSkills(originalSkills);
            }
        }

        // Disable save button initially
        binding.saveButton.setEnabled(false);

        // Add text change listeners for validation
        setupTextWatchers();
    }

    private void populateSkills(List<String> skills) {
        FlexboxLayout skillsContainer = binding.skillsContainer;
        skillsContainer.removeAllViews(); // Clear existing views except the input layout

        for (String skill : skills) {
            addSkillChip(skill, skillsContainer);
        }

        // Add the input layout at the end
        skillsContainer.addView(binding.addSkillLayout);
    }

    private void addSkillChip(String skill, FlexboxLayout container) {
        Chip chip = new Chip(getContext());
        chip.setText(skill);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.chip_background);
        chip.setTextColor(ContextCompat.getColor(getContext(), R.color.primaryColor));

        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 16, 16);
        chip.setLayoutParams(params);

        chip.setOnCloseIconClickListener(v -> {
            container.removeView(chip);
            checkChanges();
        });

        container.addView(chip, container.getChildCount() - 1); // Add before input layout
    }

    private void setupSkillsInput() {
        binding.addSkillButton.setOnClickListener(v -> {
            String newSkill = binding.skillInput.getText().toString().trim();
            if (!newSkill.isEmpty()) {
                binding.skillInput.setText("");
                addSkillChip(newSkill, binding.skillsContainer);
                checkChanges();
            }
        });
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                checkChanges();
            }
        };

        binding.fullNameEditText.addTextChangedListener(textWatcher);
        binding.phoneEditText.addTextChangedListener(textWatcher);
        binding.locationEdit.addTextChangedListener(textWatcher);

    }

    private void checkChanges() {
        boolean hasChanges = false;

        hasChanges |= !binding.fullNameEditText.getText().toString().trim()
                .equals(originalData.get("fullName"));

        hasChanges |= !binding.phoneEditText.getText().toString().trim()
                .equals(originalData.get("phone"));

        hasChanges |= !binding.locationEdit.getText().toString().trim()
                .equals(originalData.get("address"));


        // Check skills if not organizer
        if (!isOrganizer) {
            List<String> currentSkills = getCurrentSkills();
            hasChanges |= !currentSkills.equals(originalSkills);
        }

        binding.saveButton.setEnabled(hasChanges);
    }

    private List<String> getCurrentSkills() {
        List<String> currentSkills = new ArrayList<>();
        FlexboxLayout skillsContainer = binding.skillsContainer;

        for (int i = 0; i < skillsContainer.getChildCount(); i++) {
            View child = skillsContainer.getChildAt(i);
            if (child instanceof Chip) {
                currentSkills.add(((Chip)child).getText().toString());
            }
        }

        return currentSkills;
    }

    private void setupSaveButton() {
        binding.saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveProfileChanges();
            }
        });
    }

    private boolean validateInputs() {
        String name = binding.fullNameEditText.getText().toString().trim();
        String phone = binding.phoneEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!phone.isEmpty() && !Patterns.PHONE.matcher(phone).matches()) {
            Toast.makeText(getContext(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveProfileChanges() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", binding.fullNameEditText.getText().toString().trim());
        updates.put("phone", binding.phoneEditText.getText().toString().trim());
        updates.put("address", binding.locationEdit.getText().toString().trim());

        if (!isOrganizer) {
            updates.put("skills", getCurrentSkills());
        }

        db.collection("users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    binding.saveButton.setEnabled(false);

                    // Update original data
                    originalData.put("fullName", updates.get("fullName"));
                    originalData.put("phone", updates.get("phone"));
                    originalData.put("address", updates.get("address"));

                    if (!isOrganizer) {
                        originalSkills = getCurrentSkills();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    auth.signOut();
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_profileMangmentOrgFragment_to_loginFragment);

                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }
}