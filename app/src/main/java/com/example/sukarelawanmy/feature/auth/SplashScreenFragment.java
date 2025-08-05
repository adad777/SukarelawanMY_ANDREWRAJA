package com.example.sukarelawanmy.feature.auth;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.sukarelawanmy.R;
import com.example.sukarelawanmy.databinding.FragmentSplashScreenBinding;

public class SplashScreenFragment extends Fragment {

    private FragmentSplashScreenBinding binding;
    private NavController navController;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSplashScreenBinding.inflate(inflater, container, false);
        navController =NavHostFragment.findNavController(SplashScreenFragment.this);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

  // 2-second delay
    }

    @Override
    public void onResume() {
        super.onResume();
        // Simulate splash delay or animation, then navigate to LoginFragment
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            navController.navigate(R.id.action_splashScreenFragment_to_loginFragment);
        }, 2000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
