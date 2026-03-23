package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class StrengthActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHeavy, btnUpper, btnLower, btnFull, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Light";
    private String selectedType = "Full Body";
    private static final String TAG = "StrengthActivity";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_strength);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        if (timePicker != null) {
            timePicker.setMinValue(5);
            timePicker.setMaxValue(120);
            timePicker.setValue(5);
        }

        setupSelection(new Button[]{btnLight, btnModerate, btnHeavy}, btn -> selectedDifficulty = btn.getText().toString());
        setupSelection(new Button[]{btnUpper, btnLower, btnFull}, btn -> selectedType = btn.getText().toString());

        btnGo.setOnClickListener(v -> saveStrengthWorkout());
    }

    private void initViews() {
        btnLight = findViewById(R.id.btnLight);
        btnModerate = findViewById(R.id.btnModerate);
        btnHeavy = findViewById(R.id.btnHeavy);
        btnUpper = findViewById(R.id.btnUpper);
        btnLower = findViewById(R.id.btnLower);
        btnFull = findViewById(R.id.btnFull);
        btnGo = findViewById(R.id.btnGoStrength);
        timePicker = findViewById(R.id.strengthTimePicker);
        etNotes = findViewById(R.id.etStrengthNotes);
    }

    private void saveStrengthWorkout() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        Workout newWorkout = new Workout("Strength - " + selectedType, selectedDifficulty, timePicker.getValue(), etNotes.getText().toString(), 0.0);
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(Timestamp.now());

        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    // עדכון נקודות וסטריק
                    updateUserStats(currentUserId);

                    Toast.makeText(this, "Workout saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error saving workout", e));
    }

    private void updateUserStats(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // תמיד מעדכנים כוכבים (כל אימון נותן כוכבים)
                userRef.update("totalStars", FieldValue.increment(3));

                // לוגיקה חכמה לסטריק: מעלים רק אם זה האימון הראשון היום
                Timestamp lastUpdateTS = doc.getTimestamp("lastStreakUpdate");
                Date today = new Date();

                if (lastUpdateTS == null || !isSameDay(lastUpdateTS.toDate(), today)) {
                    userRef.update(
                            "streak", FieldValue.increment(1),
                            "lastStreakUpdate", new Timestamp(today)
                    );
                    Log.d(TAG, "Streak incremented!");
                } else {
                    Log.d(TAG, "Already updated streak today.");
                }
            }
        });
    }

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
                b.setBackgroundResource(android.R.color.transparent);
                b.setTextColor(Color.parseColor("#4A148C"));
            }
        }
        selected.setBackgroundResource(R.drawable.strength_selected);
        selected.setTextColor(Color.WHITE);
    }

    interface OnSelectionListener { void onSelected(Button b); }
}