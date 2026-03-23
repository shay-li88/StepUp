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

public class PilatesActivity extends AppCompatActivity {

    private Button btnBeginner, btnIntermediate, btnAdvanced, btnCore, btnFlexibility, btnFullBody, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Beginner";
    private String selectedFocus = "Core";

    private static final String TAG = "PilatesActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pilates);

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
        // 1. איסוף הנתונים מהשדות
        String type = "Pilates " + selectedFocus;
        String diff = selectedDifficulty;
        int time = timePicker.getValue();
        String notes = etNotes.getText().toString();
        double distance = 0.0;

        // --- התיקון: השגת ה-ID של המשתמש המחובר ---
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. בדיקה שהמשתמש בחר הכל (הוספת הגנה)
        if (diff.isEmpty() || selectedFocus.isEmpty()) {
            Toast.makeText(this, "Please select level and focus", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. יצירת אובייקט האימון
        Workout newWorkout = new Workout(type, diff, time, notes, distance);

        // 4. עדכון ה-userId וה-Timestamp (קריטי!)
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(com.google.firebase.Timestamp.now());

        // 5. שמירה ל-Firestore (W גדולה - Workouts)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(PilatesActivity.this, "Log saved successfully!", Toast.LENGTH_SHORT).show();

                    // מעבר למסך רשימת האימונים
                    Intent intent = new Intent(this, MyWorkoutsActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding document", e);
                    Toast.makeText(PilatesActivity.this, "Error saving log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
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