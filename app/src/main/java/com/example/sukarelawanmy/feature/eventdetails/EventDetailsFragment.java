package com.example.sukarelawanmy.feature.eventdetails;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.CalendarContract;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sukarelawanmy.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.sukarelawanmy.databinding.FragmentEventDetailsBinding;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class EventDetailsFragment extends Fragment {

    private FragmentEventDetailsBinding binding;
    private FirebaseFirestore db;
    private String eventId;
    private boolean isOrganizer = false; // Track user type
    private ShareViewModel sharedViewModel;
    ProgressDialog progress;
    private Event event;
    private boolean isJoined = false;
    private NavController navController;
    private static final int CALENDAR_PERMISSION_CODE = 100;
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEventDetailsBinding.inflate(inflater, container, false);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        navController = NavHostFragment.findNavController(this);

       progress = new ProgressDialog(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        checkUserType();

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            if (eventId != null) {
                loadEventDetails();
            }
        }

        setupClickListeners();
    }

    private void checkUserType() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String type = documentSnapshot.getString("role");
                            isOrganizer = "NGO".equals(type);
                            updateUIForUserType();
                        }
                    });
        }
    }

    private void updateUIForUserType() {
        if (isOrganizer) {
            binding.joinEventButton.setVisibility(View.GONE);
            binding.organizerButtonsContainer.setVisibility(View.VISIBLE);
        } else {
            binding.joinEventButton.setVisibility(View.VISIBLE);
            binding.organizerButtonsContainer.setVisibility(View.GONE);
        }
    }

    private void loadEventDetails() {
        db.collection("events").document(eventId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                           String id =   document.getString("creatorId");
                            if (id != null) {
                                loadOrganizerDetails(id);
                            }
                             event = document.toObject(Event.class);
                            checkedJoined(event);
                            bindEventData(document);
                        } else {
                            showError("Event not found");
                        }
                    } else {
                        showError("Error loading event");
                    }
                });
    }
    private void loadOrganizerDetails(String creatorId) {
        db.collection("users").document(creatorId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot organizerDoc = task.getResult();
                        if (organizerDoc.exists()) {
                            binding.organizerContent.setText(organizerDoc.getString("City"));
                            binding.organizerRating.setText(organizerDoc.getString("organizerName"));
                        } else {
                            binding.organizerContent.setText("Organizer not found");
                            binding.organizerRating.setVisibility(View.GONE);
                        }
                    } else {
                        binding.organizerContent.setText("Error loading organizer");
                        binding.organizerRating.setVisibility(View.GONE);
                    }
                });
    }
    private void bindEventData(DocumentSnapshot document) {
        // Header and Image
        binding.eventTitle.setText(document.getString("title"));
        binding.aboutContent.setText(document.getString("description"));

        // Date and Time Cards
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

        String eventDate = document.getString("date");
        String timestamp = document.getString("time");
        if (eventDate != null) {
            binding.eventDate.setText(eventDate);
            binding.eventTime.setText(timestamp);
        }

        // Participants Count
        Long current = document.getLong("currentParticipants") != null ? document.getLong("currentParticipants") : 0;
        Long max = document.getLong("maxParticipants") != null ? document.getLong("maxParticipants") : 0;
        binding.participantsCount.setText(getString(R.string.participants_format, current, max));

        // Duration
       // binding.eventDuration.setText(document.getString("duration"));



        // Location Info
        binding.locationContent.setText(document.getString("eventCity"));
        binding.locationAddress.setText(document.getString("location"));
        binding.organizerContent.setText(document.getString("organizerName"));
        List<String> requirements = (List<String>) document.get("requirement");
        if (requirements != null && !requirements.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String item : requirements) {
                builder.append("• ").append(item).append("\n");
            }

            if (builder.length() > 0) {
                builder.setLength(builder.length() - 1);
            }

            binding.requirements.setVisibility(View.VISIBLE); // make sure it's visible
            binding.requirements.setText(builder.toString());
        } else {
            binding.requirements.setVisibility(View.GONE);
        }

    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupClickListeners() {
        binding.backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        binding.inviteFriendsButton.setOnClickListener(v -> shareEvent());

        binding.saveForLaterButton.setOnClickListener(v -> saveEvent());

        binding.joinEventButton.setOnClickListener(v -> {
            if (isJoined) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Cancel Participation")
                        .setMessage("Are you sure you want to cancel your participation?")
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss(); // Just close the dialog
                        })
                        .setPositiveButton("Yes", (dialog, which) -> {
                                    cancelParticipation(event); // You can call your cancellation method here
                        })
                        .setCancelable(false) // Prevent closing by tapping outside
                        .show();
            } else {
                joinEvent(event);
            }
        });

        binding.viewOrganizerButton.setOnClickListener(v -> viewOrganizerProfile());

        binding.viewMapButton.setOnClickListener(v -> openMap());

        binding.editEventButton.setOnClickListener(v -> navigateToEditEvent(event));

        binding.viewParticipantsButton.setOnClickListener(v -> viewParticipants());
    }

    private void shareEvent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, binding.eventDetailsHeading.getText());
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Check out this event: " + binding.eventDetailsHeading.getText() + "\n\n" +
                        binding.aboutContent.getText() + "\n\n" +
                        "Location: " + binding.locationContent.getText());
        startActivity(Intent.createChooser(shareIntent, "Share Event"));
    }

    private void saveEvent() {
        checkCalendarPermission();
    }
    public void checkCalendarPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR},
                    CALENDAR_PERMISSION_CODE
            );
        } else {
            addEventToCalendar();
        }
    }
    public Calendar getCalendarFromStrings(String dateStr, String timeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/M/yyyy HH:mm", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        try {
            Date date = sdf.parse(dateStr + " " + timeStr); // Combine date and time
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return calendar;
    }

    public void addEventToCalendar() {
        Calendar beginTime = getCalendarFromStrings(event.getDate(), event.getTime());

        // Set duration (e.g., 1 hour)
        Calendar endTime = (Calendar) beginTime.clone();
        endTime.add(Calendar.HOUR_OF_DAY, 1); // Set event for 1 hour

        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, 1);
        values.put(CalendarContract.Events.TITLE, event.getTitle());
        values.put(CalendarContract.Events.DESCRIPTION, event.getDescription());
        values.put(CalendarContract.Events.EVENT_LOCATION, event.getLocation());
        values.put(CalendarContract.Events.DTSTART, beginTime.getTimeInMillis());
        values.put(CalendarContract.Events.DTEND, endTime.getTimeInMillis()); // ✅ Required
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID()); // ✅ Required

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR)
                == PackageManager.PERMISSION_GRANTED) {
            Uri uri = requireContext().getContentResolver().insert(CalendarContract.Events.CONTENT_URI, values);
            if (uri != null) {
                binding.saveForLaterButton.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Event added to calendar", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to add event", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALENDAR_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addEventToCalendar();
            } else {
                Toast.makeText(requireContext(), "Calendar permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void updateUIAfterJoin() {
        isJoined=true;
        binding.joinEventButton.setText("Cancel Participation");
        binding.saveForLaterButton.setVisibility(View.VISIBLE);
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(document -> {
                    try {
                        Long current = document.getLong("currentParticipants");
                        Long max = document.getLong("maxParticipants");
                        if (current != null && max != null) {
                            binding.participantsCount.setText(getString(R.string.participants_format, current, max));
                        }
                    }catch (Exception e){}

                });
    }

    private void viewOrganizerProfile() {
        // Navigate to organizer profile
    }

    private void openMap() {
        // Open location in maps
    }
    private void navigateToEditEvent(Event event) {
        Bundle args = new Bundle();
        args.putString("eventId", event.getEventId());
        navController.navigate(R.id.action_eventDetailsFragment_to_createEventFragment, args);
    }

    private void viewParticipants() {
        Bundle args = new Bundle();
        args.putString("eventId", event.getEventId());
        navController.navigate(R.id.action_eventDetailsFragment_to_participationFragment,args);
    }
    private void  checkedJoined(Event event){
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
                        updateUIAfterJoin();
                        return;
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("JoinEvent", "Error checking participation", e);
                    showToast("Error checking event status");
                });

    }
    private void showError(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
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
        showProgressDialog();

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
                    updateUIAfterJoin();
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
                        showProgressDialog();
                        batch.commit()
                                .addOnSuccessListener(aVoid -> {
                                    dismissProgressDialog();
                                    showToast("Participation cancelled successfully");
                                    binding.joinEventButton.setText("Join Event");
                                    isJoined=false;
                                    db.collection("events").document(eventId)
                                            .get()
                                            .addOnSuccessListener(document -> {
                                                Long current = document.getLong("currentParticipants");
                                                Long max = document.getLong("maxParticipants");
                                                if (current != null && max != null) {
                                                    binding.participantsCount.setText(getString(R.string.participants_format, current, max));
                                                }
                                            });
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

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
        binding=null;

    }
}