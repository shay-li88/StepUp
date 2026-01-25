package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChallengesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_challenges);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // 1. חיבור הרכיבים מה-XML לקוד ה-Java
        LinearLayout btnWorkouts = findViewById(R.id.btnworkouts);
        LinearLayout btnChallenges = findViewById(R.id.btnchallenges);
        LinearLayout btnPosts = findViewById(R.id.btnposts);
        LinearLayout btnProfile = findViewById(R.id.btnprofile);

// 2. הגדרת לחיצה למעבר לאתגרים
        btnWorkouts.setOnClickListener(v -> {
            startActivity(new Intent(ChallengesActivity.this, WorkoutsActivity.class));
            overridePendingTransition(0, 0); // מבטל אנימציה למעבר חלק
        });

// 3. הגדרת לחיצה למעבר לפוסטים
        btnPosts.setOnClickListener(v -> {
            startActivity(new Intent(ChallengesActivity.this, PostsActivity.class));
            overridePendingTransition(0, 0);
        });

// 4. הגדרת לחיצה למעבר לפרופיל
        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(ChallengesActivity.this, ProfileActivity.class));
            overridePendingTransition(0, 0);
        });
    }
}