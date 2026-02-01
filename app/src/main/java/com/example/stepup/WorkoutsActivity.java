package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_workouts);
        bottomNav.setItemIconTintList(null);
        bottomNav.setSelectedItemId(R.id.nav_workouts); // מסמן את דף הבית

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
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String type = extras.getString("type");
            String difficulty = extras.getString("difficulty");
            int time = extras.getInt("time");
            int distance = extras.getInt("distance");

            // 2. הצגת הנתונים (נניח שיש לך TextView בשם postDetails)
            TextView postDetails = findViewById(R.id.postDetails);
            String summary = "New Workout: " + type + "\n" +
                    "Level: " + difficulty + "\n" +
                    "Duration: " + time + " min\n" +
                    "Distance: " + distance + " km";

            postDetails.setText(summary);
        }
    }
}