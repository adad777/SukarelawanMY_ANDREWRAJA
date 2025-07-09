package com.example.sukarelawanmy.feature.auth;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentLoginBinding;
import com.example.sukarelawanmy.model.ShareViewModel;
import com.example.sukarelawanmy.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;

import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;
    private  NavController navController;
    private ShareViewModel sharedViewModel;
    private FirebaseFirestore db;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        sharedViewModel = new ViewModelProvider(requireActivity()).get(ShareViewModel.class);
        db = FirebaseFirestore.getInstance();
        navController =NavHostFragment.findNavController(LoginFragment.this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.loginButton.setOnClickListener(v -> {
            if (validateInputs()) {
                loginUser();
            }
        });

        binding.forgotPasswordText.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });

        binding.registerPrompt.setOnClickListener(v -> {
            navController.navigate(R.id.action_loginFragment_to_registerFragment);
        });
    }

    private boolean validateInputs() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

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

        return true;
    }

    private void loginUser() {
        showLoading(true);

        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(getContext(), "Authentication failed: " +
                                        task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void showForgotPasswordDialog() {
        String email = binding.emailEditText.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(getContext(), "Please enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(),
                                "Password reset email sent to " + email,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(),
                                "Failed to send reset email: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

//    private void updateUI(FirebaseUser user) {
//        if (user != null) {
//            // User is signed in
//           // Toast.makeText(getContext(), "Welcome " + user.getEmail(), Toast.LENGTH_SHORT).show();
//            navController.navigate(R.id.action_loginFragment_to_dashboardFragment);
//            ;
//        } else {
//            // User is signed out
//            binding.passwordEditText.setText("");
//        }
//    }
private void updateUI(FirebaseUser user) {
    if (user == null) {
        binding.passwordEditText.setText("");
        return;
    }

    db.collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String fullName = documentSnapshot.getString("fullName");
                    String email = documentSnapshot.getString("email");
                    String role = documentSnapshot.getString("role");
                    Date joinDate = documentSnapshot.getTimestamp("joinDate") != null ?
                            documentSnapshot.getTimestamp("joinDate").toDate() : null;
                    UserModel appUser = new UserModel(user.getUid(), fullName, email, role);
                    appUser.setJoinDate(joinDate);

                    sharedViewModel.setUser(appUser);

                    if ("NGO".equalsIgnoreCase(role)) {
                        navController.navigate(R.id.action_loginFragment_to_homeFragment);
                    } else {
                        navController.navigate(R.id.action_loginFragment_to_dashboardFragment); // volunteer
                    }

                } else {
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
}


    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.loginButton.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.loginButton.setEnabled(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}