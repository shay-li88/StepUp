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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FeedActivity extends AppCompatActivity {

    private static final String TAG = "FeedActivity";

    // רכיבי טקסט ונתונים
    private TextView tvHelloUser, tvStreak, tvPoints;

    // כפתורי ניווט תחתון
    private LinearLayout btnWorkouts, btnChallenges, btnPosts, btnProfile;

    // כפתורי אימונים בפיד
    private LinearLayout btnRunning, btnStrength, btnCardio, btnPilates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feed);

        // הגדרת Padding למערכת (System Bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
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

        // תפריט תחתון
        btnWorkouts = findViewById(R.id.btnworkouts);
        btnChallenges = findViewById(R.id.btnchallenges);
        btnPosts = findViewById(R.id.btnposts);
        btnProfile = findViewById(R.id.btnprofile);

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
        // --- ניווט תחתון ---
        btnWorkouts.setOnClickListener(v -> {
            // אנחנו כבר בפיד האימונים
            Toast.makeText(this, "You are already in Workouts", Toast.LENGTH_SHORT).show();
        });

        btnChallenges.setOnClickListener(v -> {
            Toast.makeText(this, "Challenges coming soon!", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, ChallengesActivity.class);
            // startActivity(intent);
        });

        btnPosts.setOnClickListener(v -> {
            Toast.makeText(this, "Posts screen clicked", Toast.LENGTH_SHORT).show();
        });

        btnProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Profile screen clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(this, ProfileActivity.class);
            // startActivity(intent);
        });

        // --- כפתורי אימונים ---
        btnRunning.setOnClickListener(v -> {
            Toast.makeText(this, "Starting Running workout...", Toast.LENGTH_SHORT).show();
        });

        btnStrength.setOnClickListener(v -> {
            Toast.makeText(this, "Starting Strength workout...", Toast.LENGTH_SHORT).show();
        });

        btnCardio.setOnClickListener(v -> {
            Toast.makeText(this, "Starting Cardio workout...", Toast.LENGTH_SHORT).show();
        });

        btnPilates.setOnClickListener(v -> {
            Toast.makeText(this, "Starting Pilates workout...", Toast.LENGTH_SHORT).show();
        });

        // בונוס: לחיצה ארוכה על השם מאפשרת להתנתק (Sign Out)
        tvHelloUser.setOnLongClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return true;
        });
    }
}