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

public class PilatesActivity extends AppCompatActivity {

    private Button btnBeginner, btnIntermediate, btnAdvanced, btnCore, btnFlexibility, btnFullBody, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Beginner";
    private String selectedFocus = "Core";
    private FirebaseFirestore db;

    private static final String TAG = "PilatesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pilates);

        db = FirebaseFirestore.getInstance();

        // קישור רכיבים
        btnBeginner = findViewById(R.id.btnBeginner);
        btnIntermediate = findViewById(R.id.btnIntermediate);
        btnAdvanced = findViewById(R.id.btnAdvanced);
        btnCore = findViewById(R.id.btnCore);
        btnFlexibility = findViewById(R.id.btnFlexibility);
        btnFullBody = findViewById(R.id.btnFullBody);
        btnGo = findViewById(R.id.btnGoPilates);
        timePicker = findViewById(R.id.pilatesTimePicker);
        etNotes = findViewById(R.id.etPilatesNotes);

        timePicker.setMinValue(5);
        timePicker.setMaxValue(120);
        timePicker.setValue(40);

        // לוגיקה לבחירת רמה
        setupSelection(new Button[]{btnBeginner, btnIntermediate, btnAdvanced}, btn -> selectedDifficulty = btn.getText().toString());

        // לוגיקה לבחירת מיקוד
        setupSelection(new Button[]{btnCore, btnFlexibility, btnFullBody}, btn -> selectedFocus = btn.getText().toString());

        btnGo.setOnClickListener(v -> savePilatesWorkout());
    }

    private void savePilatesWorkout() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        Workout newWorkout = new Workout("Pilates " + selectedFocus, selectedDifficulty, timePicker.getValue(), etNotes.getText().toString(), 0.0);
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(Timestamp.now());

        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Workout saved with ID: " + documentReference.getId());

                    // --- עדכון סטטיסטיקות משתמש (כוכבים וסטריק חכם) ---
                    updateUserStats(currentUserId);

                    Toast.makeText(PilatesActivity.this, "Workout saved! +3 Stars", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, MyWorkoutsActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(PilatesActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // הפונקציה המעודכנת לעדכון כוכבים וסטריק בצורה חכמה
    private void updateUserStats(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // 1. תמיד מוסיפים 3 כוכבים
                userRef.update("totalStars", FieldValue.increment(3));

                // 2. בדיקה אם הסטריק כבר עודכן היום
                Timestamp lastUpdateTS = doc.getTimestamp("lastStreakUpdate");
                Date today = new Date();

                if (lastUpdateTS == null || !isSameDay(lastUpdateTS.toDate(), today)) {
                    userRef.update(
                            "streak", FieldValue.increment(1),
                            "lastStreakUpdate", new Timestamp(today)
                    );
                    Log.d("Points", "Pilates: Streak incremented!");
                } else {
                    Log.d("Points", "Pilates: Streak already updated today.");
                }
            }
        }).addOnFailureListener(e -> Log.e("Points", "Error fetching user", e));
    }

    // בדיקה אם שני תאריכים הם באותו יום
    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void setupSelection(Button[] group, OnSelectionListener listener) {
        for (Button b : group) {
            if (b == null) continue;
            b.setOnClickListener(v -> {
                updateButtonUI(b, group);
                listener.onSelected(b);
            });
        }
    }

    private void updateButtonUI(Button selected, Button[] group) {
        for (Button b : group) {
            if (b != null) {
                b.setBackgroundTintList(null);
                b.setBackgroundResource(android.R.color.transparent);
                b.setTextColor(Color.parseColor("#1A4375"));
            }
        }
        selected.setBackgroundResource(R.drawable.pilates_selected);
        selected.setTextColor(Color.WHITE);
    }

    interface OnSelectionListener { void onSelected(Button b); }
}