package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepup.utils.WorkoutAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class WorkoutsActivity extends AppCompatActivity {

    private static final String TAG = "WorkoutsActivity";
    private RecyclerView recyclerView;
    private WorkoutAdapter adapter;
    private List<Workout> workoutList;
    private CardView emptyCard; // הכרטיס של "אין אימונים"
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workouts);

        // 1. הגדרת שוליים
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 2. אתחול רכיבים
        recyclerView = findViewById(R.id.recyclerViewWorkouts);
        emptyCard = findViewById(R.id.workoutCardEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        workoutList = new ArrayList<>();
        adapter = new WorkoutAdapter(workoutList);
        recyclerView.setAdapter(adapter);

        // 3. Firestore
        db = FirebaseFirestore.getInstance();
        loadWorkoutsFromFirestore();

        setupBottomNavigation();
    }

    private void loadWorkoutsFromFirestore() {
        db.collection("Workouts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        workoutList.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            Workout workout = document.toObject(Workout.class);
                            if (workout != null) {
                                workoutList.add(workout);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // אם יש נתונים - נראה את הרשימה ונסתיר את כרטיס ה-Empty
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyCard.setVisibility(View.GONE);
                    } else {
                        // אם אין נתונים - נסתיר את הרשימה ונראה את כרטיס ה-Empty
                        recyclerView.setVisibility(View.GONE);
                        emptyCard.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading workouts", e);
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_workouts);
        if (bottomNav != null) {
            bottomNav.setItemIconTintList(null);
            bottomNav.setSelectedItemId(R.id.nav_workouts);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_workouts) return true;
                if (id == R.id.nav_home) startActivity(new Intent(this, FeedActivity.class));
                else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
                else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
                else if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            });
        }
    }
}