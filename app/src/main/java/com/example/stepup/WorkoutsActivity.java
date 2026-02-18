package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class WorkoutsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WorkoutAdapter adapter;
    private List<Workout> workoutList;
    private List<Workout> fullWorkoutList;
    private CardView emptyCard;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workouts);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        recyclerView = findViewById(R.id.recyclerViewWorkouts);
        emptyCard = findViewById(R.id.workoutCardEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        workoutList = new ArrayList<>();
        fullWorkoutList = new ArrayList<>();
        adapter = new WorkoutAdapter(workoutList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadWorkoutsFromFirestore();
        setupFilterButtons();
        setupBottomNavigation();
    }

    private void setupFilterButtons() {
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> filterWorkouts("All"));
        findViewById(R.id.btnFilterRunning).setOnClickListener(v -> filterWorkouts("Running"));
        findViewById(R.id.btnFilterStrength).setOnClickListener(v -> filterWorkouts("Strength"));
        findViewById(R.id.btnFilterCardio).setOnClickListener(v -> filterWorkouts("Cardio"));
        findViewById(R.id.btnFilterPilates).setOnClickListener(v -> filterWorkouts("Pilates"));
        findViewById(R.id.btnFilterRecent).setOnClickListener(v -> filterWorkouts("Recent"));
    }

    private void loadWorkoutsFromFirestore() {
        db.collection("Workouts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    fullWorkoutList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Workout workout = document.toObject(Workout.class);
                        if (workout != null) fullWorkoutList.add(workout);
                    }
                    filterWorkouts("All");
                });
    }

    private void filterWorkouts(String criteria) {
        workoutList.clear();
        String searchCriteria = criteria.toLowerCase().trim();

        if (criteria.equals("All")) {
            workoutList.addAll(fullWorkoutList);
        } else if (criteria.equals("Recent")) {
            int limit = Math.min(fullWorkoutList.size(), 5);
            for (int i = 0; i < limit; i++) {
                workoutList.add(fullWorkoutList.get(i));
            }
        } else {
            for (Workout w : fullWorkoutList) {
                // חיפוש חכם שמתעלם מאותיות גדולות/קטנות
                if (w.getType().toLowerCase().contains(searchCriteria)) {
                    workoutList.add(w);
                }
            }
        }

        adapter.notifyDataSetChanged();

        if (workoutList.isEmpty()) {
            emptyCard.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyCard.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_workouts);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_workouts);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_workouts) return true;
                if (id == R.id.nav_home) startActivity(new Intent(this, FeedActivity.class));
                else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
                else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
                else if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
                finish();
                return true;
            });
        }
    }
}