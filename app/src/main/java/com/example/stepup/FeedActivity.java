package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class FeedActivity extends AppCompatActivity {

    private TextView tvHelloUser, tvStreak, tvPoints;
    private LinearLayout btnRunning, btnStrength, btnCardio, btnPilates;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        displayUserData();
        setupListeners();

        // הגדרת הניווט התחתון
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_feed);
        bottomNav.setItemIconTintList(null);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_workouts) startActivity(new Intent(this, MyWorkoutsActivity.class));
            else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
            overridePendingTransition(0, 0);
            return true;
        });
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String currentUserId = user.getUid();

            // שם המשתמש
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                tvHelloUser.setText("Hello, " + name + "!");
            }

            // האזנה לנתונים - שימוש ב-totalStars
            FirebaseFirestore.getInstance().collection("users").document(currentUserId)
                    .addSnapshotListener((documentSnapshot, error) -> {
                        if (error != null) return;

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Long streak = documentSnapshot.getLong("streak");
                            // שינוי ל-totalStars כאן:
                            Long stars = documentSnapshot.getLong("totalStars");

                            // עדכון הטקסט (האימוג'ים יישארו אם תעדכני את ה-XML למטה)
                            tvStreak.setText("Today's streak: " + (streak != null ? streak : 0) + " days");
                            tvPoints.setText(String.valueOf(stars != null ? stars : 0));
                        }
                    });
        }
    }

    private void setupListeners() {
        btnRunning.setOnClickListener(v -> startActivity(new Intent(this, RunningActivity.class)));
        btnStrength.setOnClickListener(v -> startActivity(new Intent(this, StrengthActivity.class)));
        btnCardio.setOnClickListener(v -> startActivity(new Intent(this, CardioActivity.class)));
        btnPilates.setOnClickListener(v -> startActivity(new Intent(this, PilatesActivity.class)));

        tvHelloUser.setOnLongClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        });
    }
}