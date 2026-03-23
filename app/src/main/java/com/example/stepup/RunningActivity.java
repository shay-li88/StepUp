package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RunningActivity extends AppCompatActivity {

    private Button btnEasy, btnMedium, btnIntense, btnGo;
    private NumberPicker timePicker, distancePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Easy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        // חיבור ה-ID מה-XML
        btnEasy = findViewById(R.id.btnEasy);
        btnMedium = findViewById(R.id.btnMedium);
        btnIntense = findViewById(R.id.btnIntense);
        btnGo = findViewById(R.id.btnGoActivity);
        timePicker = findViewById(R.id.timePicker);
        distancePicker = findViewById(R.id.distancePicker);
        etNotes = findViewById(R.id.etRunningNotes);

        // הגדרת הגלילים (Time & Distance)
        timePicker.setMinValue(1);
        timePicker.setMaxValue(120);
        timePicker.setValue(30);

        distancePicker.setMinValue(1);
        distancePicker.setMaxValue(50);
        distancePicker.setValue(4);

        // הגדרת כפתורי רמת קושי
        setupDifficulty(btnEasy);
        setupDifficulty(btnMedium);
        setupDifficulty(btnIntense);

        // כפתור שמירה ושליחה
        btnGo.setOnClickListener(v -> saveRunningWorkout());
    }

    private void saveRunningWorkout() {
        // 1. איסוף הנתונים
        String type = "Running";
        String diff = selectedDifficulty;
        int time = timePicker.getValue();
        double distance = (double) distancePicker.getValue();
        String notes = etNotes.getText().toString();

        if (diff == null || diff.isEmpty()) {
            Toast.makeText(RunningActivity.this, "Please select difficulty level", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- התיקון: השגת ה-ID של המשתמש המחובר ---
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. יצירת אובייקט האימון
        Workout newWorkout = new Workout(type, diff, time, notes, distance);

        // 3. עדכון ה-userId וה-Timestamp (קריטי להפרדה בין משתמשים)
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(com.google.firebase.Timestamp.now());

        // 4. שמירה ל-Firestore (W גדולה - Workouts)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    Log.d("RunningActivity", "Workout saved with ID: " + documentReference.getId());
                    Toast.makeText(RunningActivity.this, "Workout saved successfully!", Toast.LENGTH_SHORT).show();

                    // מעבר למסך רשימת האימונים
                    Intent intent = new Intent(RunningActivity.this, MyWorkoutsActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w("RunningActivity", "Error adding document", e);
                    Toast.makeText(RunningActivity.this, "Error saving: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupDifficulty(Button clickedBtn) {
        clickedBtn.setOnClickListener(v -> {
            resetButtons();
            clickedBtn.setBackgroundResource(R.drawable.selected_difficulty);
            clickedBtn.setTextColor(Color.WHITE);
            selectedDifficulty = clickedBtn.getText().toString();
        });
    }

    private void resetButtons() {
        Button[] btns = {btnEasy, btnMedium, btnIntense};
        for (Button b : btns) {
            if (b != null) {
                b.setBackgroundResource(android.R.color.transparent);
                b.setTextColor(Color.parseColor("#2D6A4F")); // ירוק כהה
            }
        }
    }
}