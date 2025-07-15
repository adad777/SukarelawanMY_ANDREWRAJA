package com.example.sukarelawanmy;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.core.view.WindowCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.sukarelawanmy.feature.viewmodel.ShareViewModel;
import com.example.sukarelawanmy.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;

import android.util.Log;

import androidx.navigation.NavDestination;

import com.example.sukarelawanmy.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    // Inside your Fragment or Activity (e.g., onCreateView or onCreate)



        private ActivityMainBinding binding;
        private NavController navController;
        private FirebaseAuth mAuth;
        private ShareViewModel sharedViewModel;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            sharedViewModel = new ViewModelProvider(this).get(ShareViewModel.class);

            EdgeToEdge.enable(this);
            ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            mAuth = FirebaseAuth.getInstance();

            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fragmentContainerView);
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
            }
            sharedViewModel.getUser().observe(this, userModel -> {
                if (userModel != null) {
                    // Use the user model data
                    Log.d("SharedViewModel", "User: " + userModel.getFullName());
                    // Load the menu based on user type
                    setupBottomMenuForUser(userModel);
                }
            });

        }

        private void setupBottomMenuForUser(UserModel userModel) {
            String userType = userModel.getRole(); // Implement this method to get user type: "organizer", "volunteer", etc.

            if (userType.equals("NGO")) {
                binding.bottomNavigation.getMenu().clear();
                binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_ngo);
            } else {
                binding.bottomNavigation.getMenu().clear();
                binding.bottomNavigation.inflateMenu(R.menu.menu);
            }

            setBottomNav();
        }

        private void setBottomNav() {
            binding.bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                NavDestination currentDestination = navController.getCurrentDestination();
                if (currentDestination == null) return false;

                if (itemId == R.id.nav_dashboard && currentDestination.getId() != R.id.dashboardFragment) {
                    navController.navigate(R.id.dashboardFragment);
                    return true;
                }else if (itemId == R.id.nav_home && currentDestination.getId() != R.id.homeFragment) {
                    navController.navigate(R.id.homeFragment);
                    return true;
                }
                else if (itemId == R.id.my_events && currentDestination.getId() != R.id.myEventsFragment) {
                    navController.navigate(R.id.allEventsFragment);
                    return true;
                }
                 else if (itemId == R.id.all_events && currentDestination.getId() != R.id.allEventsFragment) {
                    navController.navigate(R.id.allEventsFragment);
                    binding.bottomNavigation.setVisibility(android.view.View.VISIBLE);

                    return true;
                }


                else if (itemId == R.id.nav_profile && currentDestination.getId() != R.id.profileMangmentOrgFragment) {
                    navController.navigate(R.id.profileMangmentOrgFragment);
                    return true;
                }

                return true;
            });

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.dashboardFragment ||
                        id == R.id.myEventsFragment ||
                        id == R.id.homeFragment ||
                        id == R.id.allEventsFragment ||
                        id == R.id.profileMangmentOrgFragment
                ) {
                    binding.bottomNavigation.setVisibility(android.view.View.VISIBLE);
                } else {
                    binding.bottomNavigation.setVisibility(android.view.View.GONE);
                }
            });
        }



        @Override
        protected void onStart() {
            super.onStart();
        }

        @Override
        public boolean onSupportNavigateUp() {
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
    }
