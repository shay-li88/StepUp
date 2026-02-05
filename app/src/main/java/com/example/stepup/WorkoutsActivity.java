package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class WorkoutsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workouts);

        // 1. הגדרת שוליים (למניעת חיתוך הטקסט על ידי הסטטוס-בר)
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 2. חיבור ה-TextView מה-XML
        TextView postDetails = findViewById(R.id.postDetails);

        // 3. ניווט תחתון
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
                return true;
            });
        }

        // 4. קבלת נתונים מהאימון והצגתם
        // בתוך ה-onCreate של WorkoutsActivity.java
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String type = extras.getString("type", "");
            String difficulty = extras.getString("difficulty", "");
            int time = extras.getInt("time", 0);

            // קבלת ההערות - ודאי שהמפתח זהה למה ששלחת!
            String notesText = extras.getString("notes", "");

            String summary = "New Workout: " + type + "\n" +
                    "Level: " + difficulty + "\n" +
                    "Duration: " + time + " min";

            // אם יש הערות, נוסיף אותן לסוף המחרוזת
            if (notesText != null && !notesText.isEmpty()) {
                summary += "\n\nNotes: " + notesText;
            }

            postDetails.setText(summary);

        }
    }
}