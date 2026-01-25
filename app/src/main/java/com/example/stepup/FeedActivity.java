package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";

    // רכיבי טקסט ונתונים
    private TextView tvHelloUser, tvStreak, tvPoints;

    // כפתורי ניווט תחתון
    private LinearLayout btnWorkouts, btnChallenges, btnPosts, btnProfile, btnHome;

    // כפתורי אימונים בפיד
    private LinearLayout btnRunning, btnStrength, btnCardio, btnPilates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_feed);
        bottomNav.setItemIconTintList(null);
        bottomNav.setSelectedItemId(R.id.nav_home); // מסמן את דף ההודעות

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            if (id == R.id.nav_workouts) startActivity(new Intent(this, WorkoutsActivity.class));
            else if (id == R.id.nav_posts) startActivity(new Intent(this, PostsActivity.class));
            else if (id == R.id.nav_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (id == R.id.nav_challenges) startActivity(new Intent(this, ChallengesActivity.class));
            overridePendingTransition(0, 0);
            return true;
        });
        initViews();       // קישור כל ה-IDs
        displayUserData(); // הצגת שם המשתמש מ-Firebase
        setupListeners();  // הגדרת לחיצות

    }

    private void initViews() {
        // טקסטים
        tvHelloUser = findViewById(R.id.tvHelloUser);
        tvStreak = findViewById(R.id.tvStreak);
        tvPoints = findViewById(R.id.tvPoints);



        // כפתורי אימון
        btnRunning = findViewById(R.id.btnRunning);
        btnStrength = findViewById(R.id.btnStrength);
        btnCardio = findViewById(R.id.btnCardio);
        btnPilates = findViewById(R.id.btnPilates);
    }

    private void displayUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();

            // אם קיים שם משתמש, נציג אותו. אם לא (במקרה של משתמש ישן), נציג חלק מהאימייל.
            if (name != null && !name.isEmpty()) {
                tvHelloUser.setText("Hello, " + name + "!");
            } else {
                String email = user.getEmail();
                if (email != null) {
                    tvHelloUser.setText("Hello, " + email.split("@")[0] + "!");
                }
            }
        }
    }

    private void setupListeners() {

        tvHelloUser.setOnLongClickListener(v -> {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
        return true;
    });
        }}