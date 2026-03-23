package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class FeedActivity extends AppCompatActivity {

    private TextView tvHelloUser, tvStreak, tvPoints;
    private LinearLayout btnRunning, btnStrength, btnCardio, btnPilates;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // תיקון ה-Padding עבור מסכים עם מגרעת (Notch)
        // השתמשתי ב-main_layout שנוסיף ל-XML מיד
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
        displayUserData();
        checkAndResetStreak();
        setupBottomNavigation();
    }

    private void initViews() {
        tvHelloUser = findViewById(R.id.tvHelloUser);
        tvStreak = findViewById(R.id.tvStreak);
        tvPoints = findViewById(R.id.tvPoints);
        btnRunning = findViewById(R.id.btnRunning);
        btnStrength = findViewById(R.id.btnStrength);
        btnCardio = findViewById(R.id.btnCardio);
        btnPilates = findViewById(R.id.btnPilates);
    }

    private void displayUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String currentUserId = user.getUid();
            String name = user.getDisplayName();
            tvHelloUser.setText("Hello, " + (name != null && !name.isEmpty() ? name : "User") + "!");

            db.collection("users").document(currentUserId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) return;
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Long streak = documentSnapshot.getLong("streak");
                            Long stars = documentSnapshot.getLong("totalStars");

                            tvStreak.setText("Today's streak: " + (streak != null ? streak : 0) + " days");
                            tvPoints.setText(String.valueOf(stars != null ? stars : 0));
                        }
                    });
        }
    }

    private void checkAndResetStreak() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("Workouts")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot lastDoc = queryDocumentSnapshots.getDocuments().get(0);

                        // --- התיקון המרכזי כאן ---
                        Object timestampObj = lastDoc.get("timestamp");
                        Date lastWorkoutDate = null;

                        if (timestampObj instanceof Timestamp) {
                            // אם זה פורמט Timestamp (החדשים)
                            lastWorkoutDate = ((Timestamp) timestampObj).toDate();
                        } else if (timestampObj instanceof Long || timestampObj instanceof Double) {
                            // אם זה פורמט Number (הישנים)
                            lastWorkoutDate = new Date(((Number) timestampObj).longValue());
                        }

                        if (lastWorkoutDate != null) {
                            validateStreakLogic(uid, lastWorkoutDate);
                        }
                        // ------------------------

                    } else {
                        // אם אין אימונים בכלל, לאפס ל-0
                        db.collection("users").document(uid).update("streak", 0);
                    }
                })
                .addOnFailureListener(e -> Log.e("Streak", "Error checking streak: " + e.getMessage()));
    }

    private void validateStreakLogic(String uid, Date lastWorkoutDate) {
        Date now = new Date();
        long diffInMillies = Math.abs(now.getTime() - lastWorkoutDate.getTime());
        long diffInDays = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        // אם עברו יותר מיומיים (48 שעות) מהאימון האחרון
        if (diffInDays >= 2) {
            db.collection("users").document(uid).update("streak", 0);
            Log.d("Streak", "Streak reset - more than 48 hours passed");
        }
    }

    private void setupListeners() {
        btnRunning.setOnClickListener(v -> startActivity(new Intent(this, RunningActivity.class)));
        btnStrength.setOnClickListener(v -> startActivity(new Intent(this, StrengthActivity.class)));
        btnCardio.setOnClickListener(v -> startActivity(new Intent(this, CardioActivity.class)));
        btnPilates.setOnClickListener(v -> startActivity(new Intent(this, PilatesActivity.class)));

        tvHelloUser.setOnLongClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_feed);
        if (bottomNav != null) {
            bottomNav.setItemIconTintList(null);
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;

                Intent intent = null;
                if (id == R.id.nav_workouts) intent = new Intent(this, MyWorkoutsActivity.class);
                else if (id == R.id.nav_posts) intent = new Intent(this, PostsActivity.class);
                else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);
                else if (id == R.id.nav_challenges) intent = new Intent(this, ChallengesActivity.class);

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                }
                return true;
            });
        }
    }
}