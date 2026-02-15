package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast; // ייבוא קריטי!
import androidx.appcompat.app.AppCompatActivity;

public class AddPostsActivity extends AppCompatActivity {

    private String selectedWorkoutType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_posts);

        EditText etTitle = findViewById(R.id.etTitle);
        EditText etContent = findViewById(R.id.etContent);
        Button btnPost = findViewById(R.id.btnPost);

        // לוגיקה לבחירת סוג אימון (לפי הכפתורים הצבעוניים)
        findViewById(R.id.btnRunning).setOnClickListener(v -> selectedWorkoutType = "Running");
        findViewById(R.id.btnStrength).setOnClickListener(v -> selectedWorkoutType = "Strength");
        findViewById(R.id.btnCardio).setOnClickListener(v -> selectedWorkoutType = "Cardio");
        findViewById(R.id.btnPilates).setOnClickListener(v -> selectedWorkoutType = "Pilates");

        btnPost.setOnClickListener(v -> {
            String title = etTitle.getText().toString();
            String content = etContent.getText().toString();

            if (title.isEmpty() || selectedWorkoutType.isEmpty()) {
                // תיקון שגיאת ה-makeText
                Toast.makeText(AddPostsActivity.this, "Please enter title and select workout", Toast.LENGTH_SHORT).show();
                return;
            }

            // מעבר לדף האימון הרלוונטי או שמירה ישירה
            Intent intent;
            switch (selectedWorkoutType) {
                case "Pilates": intent = new Intent(this, PilatesActivity.class); break;
                case "Running": intent = new Intent(this, RunningActivity.class); break;
                case "Strength": intent = new Intent(this, StrengthActivity.class); break;
                default: intent = new Intent(this, CardioActivity.class); break;
            }

            // העברת הנתונים כפי שראינו בצילומי המסך שלך
            intent.putExtra("post_title", title);
            intent.putExtra("post_content", content);
            startActivity(intent);
        });
    }
}