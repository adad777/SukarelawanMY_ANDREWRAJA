package com.example.sukarelawanmy.feature.EventOrganizer;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import com.example.sukarelawanmy.databinding.FragmentCreateEventBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateEventFragment extends Fragment {

    private FragmentCreateEventBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storageRef;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateEventBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("event_images");

        setupDateAndTimePickers();
        setupImageUpload();
        setupCreateButton();
    }

    private void setupDateAndTimePickers() {
        // Date Picker
        binding.eventDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        binding.eventDate.setText(date);
                    }, year, month, day);
            datePicker.show();
        });

        // Time Picker
        binding.eventTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                    (view, selectedHour, selectedMinute) -> {
                        String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                        binding.eventTime.setText(time);
                    }, hour, minute, true);
            timePicker.show();
        });
    }

    private void setupImageUpload() {
        binding.imageCard.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            try {
                imageUri = data.getData();
                binding.eventImage.setImageURI(imageUri);
                binding.addImageIcon.setVisibility(View.GONE);

                // Optional: Get file info for debugging
                ContentResolver contentResolver = requireContext().getContentResolver();
                String mimeType = contentResolver.getType(imageUri);
                Log.d("ImageInfo", "Selected image type: " + mimeType);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                Log.e("ImageError", "Error loading image", e);
            }
        }
    }

    private void setupCreateButton() {
        binding.createButton.setOnClickListener(v -> {
            if (validateInputs()) {
                if (imageUri != null) {
                    uploadImageAndCreateEvent();
                } else {
                    createEventInFirestore("");
                }
            }
        });
    }

    private boolean validateInputs() {
        if (binding.titleInputLayout.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter event title", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.locationInputLayout.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please enter location", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.eventDate.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please select date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.eventTime.getText().toString().trim().isEmpty()) {
            Toast.makeText(requireContext(), "Please select time", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void uploadImageAndCreateEvent() {
        // Check if imageUri is valid
        if (imageUri == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a unique filename
        String filename = "event_" + System.currentTimeMillis() + getFileExtension(imageUri);

        // Create reference to storage location
        StorageReference fileRef = storageRef.child(filename);

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setTitle("Uploading Image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Upload file
        fileRef.putFile(imageUri)
                .addOnProgressListener(taskSnapshot -> {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    progressDialog.setMessage("Uploaded " + (int) progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        progressDialog.dismiss();
                        createEventInFirestore(uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("UploadError", "Error uploading image", e);
                });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return "." + mime.getExtensionFromMimeType(contentResolver.getType(uri));
    }
    private void createEventInFirestore(String imageUrl) {
        String ngoId = auth.getCurrentUser().getUid();
        String title = binding.titleInputLayout.getText().toString().trim();
        String location = binding.locationInputLayout.getText().toString().trim();
        String date = binding.eventDate.getText().toString().trim();
        String time = binding.eventTime.getText().toString().trim();

        Map<String, Object> event = new HashMap<>();
        event.put("title", title);
        event.put("location", location);
        event.put("date", date);
        event.put("time", time);
        event.put("imageUrl", imageUrl);
        event.put("ngoId", ngoId);
        event.put("volunteerCount", 0);
        event.put("createdAt", System.currentTimeMillis());

        db.collection("events")
                .add(event)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(requireContext(), "Event created successfully", Toast.LENGTH_SHORT).show();
                    // Navigate back or clear form
                    requireActivity().onBackPressed();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error creating event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}