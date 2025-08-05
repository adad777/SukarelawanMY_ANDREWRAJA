package com.example.sukarelawanmy.feature.auth;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentRegisterBinding;
import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.example.sukarelawanmy.model.UserModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.lifecycle.ViewModelProvider;


import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private NavController navController;
    private ShareViewModel userViewModel;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        navController = NavHostFragment.findNavController(RegisterFragment.this);

        userViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.createAccountButton.setOnClickListener(v -> {
            if (validateInputs()) {
                hideKeyboard();
                registerUser();
            }
        });

        binding.loginPrompt.setOnClickListener(v -> {
            hideKeyboard();
           // navController.navigate(R.id.action_registerFragment_to_loginFragment);
            navController.popBackStack();

        });
    }

    private boolean validateInputs() {
        String fullName = binding.fullNameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();
        int selectedRoleId = binding.roleRadioGroup.getCheckedRadioButtonId();

        if (fullName.isEmpty()) {
            binding.fullNameEditText.setError("Full name is required");
            binding.fullNameEditText.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            binding.emailEditText.setError("Email is required");
            binding.emailEditText.requestFocus();
            return false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailEditText.setError("Please enter a valid email");
            binding.emailEditText.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            binding.passwordEditText.setError("Password is required");
            binding.passwordEditText.requestFocus();
            return false;
        } else if (password.length() < 6) {
            binding.passwordEditText.setError("Password must be at least 6 characters");
            binding.passwordEditText.requestFocus();
            return false;
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordEditText.setError("Please confirm your password");
            binding.confirmPasswordEditText.requestFocus();
            return false;
        } else if (!password.equals(confirmPassword)) {
            binding.confirmPasswordEditText.setError("Passwords don't match");
            binding.confirmPasswordEditText.requestFocus();
            return false;
        }

        if (selectedRoleId == -1) {
            Toast.makeText(getContext(), "Please select a role", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerUser() {
        showLoading(true);

        String fullName = binding.fullNameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String role = binding.volunteerRadioButton.isChecked() ? "Volunteer" : "NGO";

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // Registration success
                        FirebaseUser user = mAuth.getCurrentUser();
                         saveUserToFirestore(user, fullName, role);
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            saveUserToFirestore(user, fullName, role);
                                            showMessageDialog("Registration successful!\nA verification email has been sent to " + email + ". Please verify before logging in.");
                                            FirebaseAuth.getInstance().signOut(); // Log out until email is verified
                                            navController.popBackStack(); // Return to Login screen
                                        } else {
                                            showMessageDialog("Failed to send verification email: " + verifyTask.getException().getMessage());
                                        }
                                    });
                        }
                    } else {showMessageDialog("Registration failed: " +
                        task.getException().getMessage());
                    }
                });
    }
    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireView().getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(requireView().getWindowToken(), 0);
        }
    }

    private void saveUserToFirestore(FirebaseUser user, String fullName, String role) {
        if (user == null) return;

        // Create a new user document in Firestore
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("fullName", fullName);
        userData.put("email", user.getEmail());
        userData.put("role", role);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("joinDate", new Timestamp(new Date()));

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                    } else {
                        Toast.makeText(getContext(), "Failed to save user data: " +
                                task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }

                });
    }

    private void navigateToMainScreen() {
        // Navigate to main activity or home fragment
        // Example:
        // Navigation.findNavController(requireView()).navigate(
        //     RegisterFragmentDirections.actionRegisterFragmentToHomeFragment()
        // );

        // Or if using Activity:
        // startActivity(new Intent(getActivity(), MainActivity.class));
        // requireActivity().finish();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.createAccountButton.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.createAccountButton.setEnabled(true);
        }
    }
    private void showMessageDialog( String message) {
        new AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }
}