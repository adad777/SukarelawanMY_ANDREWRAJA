package com.example.sukarelawanmy.feature.auth;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentLoginBinding;
import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.example.sukarelawanmy.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;

import android.view.inputmethod.InputMethodManager;
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
    ProgressDialog progress;
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
        progress = new ProgressDialog(requireContext());
        navController =NavHostFragment.findNavController(LoginFragment.this);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.loginButton.setOnClickListener(v -> {
            if (validateInputs()) {
                hideKeyboard();
                loginUser();
            }
        });

        binding.forgotPasswordText.setOnClickListener(v -> {
            hideKeyboard();
            showForgotPasswordDialog();
        });

        binding.registerPrompt.setOnClickListener(v -> {
            hideKeyboard();
            navController.navigate(R.id.action_loginFragment_to_registerFragment);
        });

        binding.togglePassword.setOnClickListener(v -> {
            int selection = binding.passwordEditText.getSelectionEnd();
            if (binding.passwordEditText.getTransformationMethod() instanceof PasswordTransformationMethod) {
                binding.passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.togglePassword.setImageResource(R.drawable.ic_visibility);
            } else {
                binding. passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.togglePassword.setImageResource(R.drawable.ic_visibility_off);
            }
            binding.passwordEditText.setSelection(selection);
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
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null && !user.isEmailVerified()) {
                     //   if (false) {
                            updateUI(user);
                            showResendVerificationDialog(user);
                           mAuth.signOut(); // prevent access until verified
                        } else {
                            // Email verified — proceed
                            updateUI(user);
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                           showMessageDialog("Email or password is incorrect. Try again or reset your password");
                        updateUI(null);
                    }
                });
    }
    private void showResendVerificationDialog(FirebaseUser user) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Email not verified")
                .setMessage("Your email address is not verified. Please verify to continue.")
                .setPositiveButton("Resend Email", (dialog, which) -> {
                    user.sendEmailVerification()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "Verification email sent to " + user.getEmail(), Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(getContext(), "Failed to resend email: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
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
                        showMessageDialog("Password reset email sent to " + email);

                    } else {
                        Toast.makeText(getContext(),
                                "Failed to send reset email: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
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
                    showMessageDialog("User data not found");
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
}

    private void showMessageDialog( String message) {
        new AlertDialog.Builder(requireContext())
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireView().getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(requireView().getWindowToken(), 0);
        }
    }


    private void showLoading(boolean isLoading) {
        progress.setMessage("Authenticating... hang tight!");
        progress.setCancelable(false);

        if (isLoading) {
            progress.show();
        } else {
            if (progress != null && progress.isShowing()) {
                progress.dismiss();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}