package com.example.sukarelawanmy.feature.EventOrganizer;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.sukarelawanmy.R;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.sukarelawanmy.databinding.FragmentCreateEventBinding;
import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storageRef;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private String eventId;
    private boolean isEditMode = false;
    private String currentImageUrl = "";
    private NavController navController;
    ProgressDialog progress;
    private ShareViewModel userViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("event_images");
        userViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);

        // Check if we're editing an existing event
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            isEditMode = eventId != null;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(CreateEventFragment.this);
         progress = new ProgressDialog(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        setupUI();
        setupClickListeners();

        if (isEditMode) {
            loadEventData();
            binding.createButton.setText("Update Event");
        } else {
            // Initialize with default values for new event
            binding.checkboxRequiresApproval.setChecked(true); // Default to requiring approval
            binding.maxParticipants.setText(""); // 0 means no limit
        }
    }

    private void setupUI() {
        // Set appropriate title
        binding.title.setText(isEditMode ? "Edit Event" : "Create Event");

        // Initialize date and time pickers
        setupDateTimePickers();
    }

    private void setupDateTimePickers() {
        // Date Picker
        binding.eventDate.setOnClickListener(v -> showDatePicker());

        // Time Picker
        binding.eventTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String date = day + "/" + (month + 1) + "/" + year;
                    binding.eventDate.setText(date);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (view, hour, minute) -> {
                    String time = String.format("%02d:%02d", hour, minute);
                    binding.eventTime.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true);
        timePicker.show();
    }

    private void setupClickListeners() {
        // Image upload
        binding.imageCard.setOnClickListener(v -> openImagePicker());

        // Create/Update button
        binding.createButton.setOnClickListener(v -> {
            if (validateInputs()) {
                if (isEditMode) {
                    updateEvent();
                } else {
                    createEvent();
                }
            }
        });
        // 1. Reference using ViewBinding
        binding.addRequirementButton.setOnClickListener(v -> {
            String text = binding.newRequirement.getText().toString().trim();

            if (!text.isEmpty()) {
                Chip chip = new Chip(requireContext());
                chip.setText(text);
                chip.setCloseIconVisible(true);
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.dark_text));
                chip.setChipStrokeColorResource(R.color.orange);
                chip.setChipStrokeWidth(1f);

                // Remove chip on close icon click
                chip.setOnCloseIconClickListener(close -> binding.requirementsChipGroup.removeView(chip));

                binding.requirementsChipGroup.addView(chip);
                binding.newRequirement.setText(""); // Clear input
            } else {
                binding.newRequirement.setError("Enter a requirement");
            }
        });

    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Glide.with(requireContext())
                        .load(imageUri)
                        .into(binding.eventImage);
                binding.uploadImageButton.setVisibility(View.GONE);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                Log.e("ImageError", "Error loading image", e);
            }
        }
    }

    private boolean validateInputs() {
        if (binding.eventTitle.getText().toString().trim().isEmpty()) {
            showError("Please enter event title");
            return false;
        }
        if (binding.eventCity.getText().toString().trim().isEmpty()) {
            showError("Please enter event city");
            return false;
        }

        if (binding.eventLocation.getText().toString().trim().isEmpty()) {
            showError("Please enter location");
            return false;
        }

        if (binding.eventDate.getText().toString().trim().isEmpty()) {
            showError("Please select date");
            return false;
        }
        if (binding.eventTime.getText().toString().trim().isEmpty()) {
            showError("Please select time");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadEventData() {
        showProgress("Loading event...");

        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    dismissProgress();
                    if (documentSnapshot.exists()) {
                        populateForm(documentSnapshot);
                    } else {
                        showError("Event not found");
                    }
                })
                .addOnFailureListener(e -> {
                    dismissProgress();
                    showError("Failed to load event");
                    Log.e("FirestoreError", "Error loading event", e);
                });
    }

    private void populateForm(DocumentSnapshot document) {
        // Basic Fields
        binding.eventTitle.setText(document.getString("title"));
        binding.eventDescription.setText(document.getString("description"));
        binding.eventLocation.setText(document.getString("location"));
        binding.eventCity.setText(document.getString("eventCity"));
        binding.eventDate.setText(document.getString("date"));
        binding.eventTime.setText(document.getString("time"));
        binding.eventCategory.setText(document.getString("eventCategory"));

        // Max Participants
        Long maxParticipants = document.getLong("maxParticipants");
        if (maxParticipants != null) {
            binding.maxParticipants.setText(String.valueOf(maxParticipants));
        }

        // Requires Approval (if using checkbox later)
        // Boolean requiresApproval = document.getBoolean("requiresApproval");
        // if (requiresApproval != null) {
        //     binding.checkboxRequiresApproval.setChecked(requiresApproval);
        // }

        // Load image if exists
        currentImageUrl = document.getString("imageUrl");
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            Glide.with(requireContext())
                    .load(currentImageUrl)
                    .into(binding.eventImage);
            binding.uploadImageButton.setVisibility(View.GONE);
        }

        // Populate Requirements (List<String>)
        if (document.contains("requirement")) {
            List<String> requirements = (List<String>) document.get("requirement");
            if (requirements != null) {
                binding.requirementsChipGroup.removeAllViews();
                for (String req : requirements) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(req);
                    chip.setCloseIconVisible(true);
                    chip.setOnCloseIconClickListener(v -> binding.requirementsChipGroup.removeView(chip));
                    binding.requirementsChipGroup.addView(chip);
                }
            }
        }
    }

    private void createEvent() {
        if (validateInputs()) {
            if (false) {
                uploadImageAndCreateEvent();
            } else {
                createEventInFirestore(""); // Empty string for no image
            }
        }
    }
    private void updateEvent() {
        if (imageUri != null) {
            uploadImageAndUpdateEvent();
        } else {
            updateEventInFirestore(currentImageUrl);
        }
    }

    private void uploadImageAndCreateEvent() {
        showProgress("Uploading image...");

        String filename = "event_" + System.currentTimeMillis() + getFileExtension(imageUri);
        StorageReference fileRef = storageRef.child(filename);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        createEventInFirestore(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    dismissProgress();
                    showError("Image upload failed");
                    Log.e("UploadError", "Error uploading image", e);
                });
    }


    private void createEventInFirestore(String imageUrl) {
        showProgress("Creating event...");

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            dismissProgress();
            showError("User not authenticated");
            return;
        }

        // Create a new document reference to auto-generate ID
        DocumentReference eventRef = db.collection("events").document();
        String eventId = eventRef.getId(); // Get the auto-generated ID

        Map<String, Object> event = createEventDataMap(imageUrl, user.getUid(), eventId);

        eventRef.set(event)
                .addOnSuccessListener(aVoid -> {
                    handleSuccess("Event created successfully");
                    // Optionally create subcollections or related documents here
                })
                .addOnFailureListener(e -> handleFailure("Failed to create event", e));
    }

    private Map<String, Object> createEventDataMap(String imageUrl, String userId, String eventId) {
        Map<String, Object> event = new HashMap<>();
        List<String> requirements = new ArrayList<>();
        event.put("eventId", eventId); // Store the auto-generated ID
        event.put("title", binding.eventTitle.getText().toString().trim());
        event.put("description", binding.eventDescription.getText().toString().trim());
        event.put("location", binding.eventLocation.getText().toString().trim());
        event.put("eventCity", binding.eventCity.getText().toString().trim());
        event.put("date", binding.eventDate.getText().toString().trim());
        event.put("time", binding.eventTime.getText().toString().trim());
        event.put("imageUrl", imageUrl);
        event.put("ngoId", userId);
        event.put("status", "open");
        event.put("timestamp", FieldValue.serverTimestamp());
        event.put("createdAt", FieldValue.serverTimestamp());
        event.put("eventCategory", binding.eventCategory.getText().toString().trim());
//        event.put("requiresApproval", binding.checkboxRequiresApproval.isChecked());
        event.put("requiresApproval", false);
        event.put("organizerName", userViewModel.getUser().getValue().getFullName());
        event.put("requirement", getAllRequirements());
        event.put("currentParticipants", 0);
        event.put("maxParticipants", getMaxParticipantsFromInput());
        return event;
    }
    private List<String> getAllRequirements() {
        List<String> requirements = new ArrayList<>();
        int chipCount = binding.requirementsChipGroup.getChildCount();

        for (int i = 0; i < chipCount; i++) {
            View chipView = binding.requirementsChipGroup.getChildAt(i);
            if (chipView instanceof Chip) {
                String text = ((Chip) chipView).getText().toString();
                requirements.add(text);
            }
        }
        return requirements;
    }

    private int getMaxParticipantsFromInput() {
        try {
            String input = binding.maxParticipants.getText().toString().trim();
            return input.isEmpty() ? 0 : Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0; // 0 means no limit
        }
    }
    private void uploadImageAndUpdateEvent() {
        showProgress("Updating image...");

        // First delete old image if exists
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            FirebaseStorage.getInstance().getReferenceFromUrl(currentImageUrl)
                    .delete()
                    .addOnSuccessListener(aVoid -> uploadNewImage())
                    .addOnFailureListener(e -> {
                        Log.e("ImageDelete", "Failed to delete old image", e);
                        uploadNewImage(); // Try to upload new image anyway
                    });
        } else {
            uploadNewImage();
        }
    }

    private void uploadNewImage() {
        String filename = "event_" + System.currentTimeMillis() + getFileExtension(imageUri);
        StorageReference fileRef = storageRef.child(filename);

        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updateEventInFirestore(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    dismissProgress();
                    showError("Image upload failed");
                    Log.e("UploadError", "Error uploading image", e);
                });
    }

    private void updateEventInFirestore(String imageUrl) {
        showProgress("Updating event...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", binding.eventTitle.getText().toString().trim());
        updates.put("description", binding.eventDescription.getText().toString().trim());
        updates.put("location", binding.eventLocation.getText().toString().trim());
        updates.put("eventCity", binding.eventCity.getText().toString().trim());
        updates.put("date", binding.eventDate.getText().toString().trim());
        updates.put("time", binding.eventTime.getText().toString().trim());
        updates.put("eventCategory", binding.eventCategory.getText().toString().trim());
        updates.put("requirement", getAllRequirements());
        updates.put("maxParticipants", getMaxParticipantsFromInput());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        if (!imageUrl.isEmpty()) {
            updates.put("imageUrl", imageUrl);
        }

        db.collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(aVoid -> handleSuccess("Event updated successfully"))
                .addOnFailureListener(e -> handleFailure("Failed to update event", e));
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return "." + mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void handleSuccess(String message) {
        dismissProgress();
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        navController.popBackStack();
    }

    private void handleFailure(String message, Exception e) {
        dismissProgress();
        showError(message);
        Log.e("FirestoreError", message, e);
    }

    private void showProgress(String message) {
        progress.setMessage(message);
        progress.setCancelable(false);
        progress.show();
    }

    private void dismissProgress() {
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