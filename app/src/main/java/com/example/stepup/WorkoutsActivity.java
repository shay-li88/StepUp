package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
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

        // 1. הגדרת שוליים למניעת חיתוך על ידי הסטטוס-בר
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 2. חיבור רכיבי ה-UI מה-Layout
        CardView workoutCard = findViewById(R.id.workoutCard);
        TextView postDetails = findViewById(R.id.postDetails);

        // 3. הגדרת ניווט תחתון
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

        // 4. קבלת נתונים מהאימון ועיצוב דינמי לפי סוג האימון
        Bundle extras = getIntent().getExtras();
        if (extras != null && postDetails != null && workoutCard != null) {
            String type = extras.getString("type", "");
            String difficulty = extras.getString("difficulty", "");
            int time = extras.getInt("time", 0);
            String notes = extras.getString("notes", "");

            int cardColor;
            int textColor;

            // התאמת צבעים: רקע בהיר וטקסט כהה תואם
            if (type.contains("Strength")) {
                cardColor = Color.parseColor("#F3E5F5"); // סגול בהיר מאוד (רקע)
                textColor = Color.parseColor("#4A148C"); // סגול כהה עמוק (טקסט)
            } else if (type.contains("Pilates")) {
                cardColor = Color.parseColor("#E3F2FD"); // כחול בהיר מאוד (רקע)
                textColor = Color.parseColor("#1A4375"); // כחול כהה עמוק (טקסט)
            } else if (type.contains("Cardio")) {
                cardColor = Color.parseColor("#FFEBEE"); // ורוד/אדום בהיר מאוד (רקע)
                textColor = Color.parseColor("#B71C1C"); // אדום יין כהה (טקסט)
            } else if (type.contains("Running")) {
                cardColor = Color.parseColor("#D8F3DC"); // ירוק בהיר מאוד (רקע)
                textColor = Color.parseColor("#1B4332"); // ירוק כהה עמוק (טקסט)
            } else {
                cardColor = Color.WHITE;
                textColor = Color.BLACK;
            }

            // החלת הצבעים שנבחרו על הכרטיס והטקסט
            workoutCard.setCardBackgroundColor(cardColor);
            postDetails.setTextColor(textColor);

            // בניית טקסט הסיכום כולל ההערות
            StringBuilder summary = new StringBuilder();
            summary.append("New Workout: ").append(type).append("\n");
            summary.append("Level: ").append(difficulty).append("\n");
            summary.append("Duration: ").append(time).append(" min");

            if (notes != null && !notes.trim().isEmpty()) {
                summary.append("\n\nNotes: ").append(notes);
            }

            postDetails.setText(summary.toString());
        }
    }
}