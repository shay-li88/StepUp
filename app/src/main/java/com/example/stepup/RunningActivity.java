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

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class RunningActivity extends AppCompatActivity {

    private Button btnEasy, btnMedium, btnIntense, btnGo;
    private NumberPicker timePicker, distancePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Easy";
    private FirebaseFirestore db;
    private static final String TAG = "RunningActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);

        db = FirebaseFirestore.getInstance();

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
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        Workout newWorkout = new Workout("Running", selectedDifficulty, timePicker.getValue(), etNotes.getText().toString(), (double) distancePicker.getValue());
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(Timestamp.now());

        // שמירת האימון ב-Workouts
        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Workout saved with ID: " + documentReference.getId());

                    // --- עדכון סטטיסטיקות משתמש (כוכבים וסטריק חכם) ---
                    updateUserStats(currentUserId);

                    Toast.makeText(RunningActivity.this, "Workout saved!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(RunningActivity.this, MyWorkoutsActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(RunningActivity.this, "Error saving: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // הפונקציה המעודכנת לעדכון כוכבים וסטריק
    private void updateUserStats(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // 1. תמיד מוסיפים 3 כוכבים על כל אימון
                userRef.update("totalStars", FieldValue.increment(3));

                // 2. לוגיקה לסטריק: רק אם זה האימון הראשון היום
                Timestamp lastUpdateTS = doc.getTimestamp("lastStreakUpdate");
                Date today = new Date();

                if (lastUpdateTS == null || !isSameDay(lastUpdateTS.toDate(), today)) {
                    userRef.update(
                            "streak", FieldValue.increment(1),
                            "lastStreakUpdate", new Timestamp(today)
                    );
                    Log.d("Points", "Streak incremented for today!");
                } else {
                    Log.d("Points", "Streak already updated today, skipping increment.");
                }
            }
        }).addOnFailureListener(e -> Log.e("Points", "Error fetching user for update", e));
    }

    // פונקציית עזר לבדיקה אם מדובר באותו יום קלנדרי
    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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