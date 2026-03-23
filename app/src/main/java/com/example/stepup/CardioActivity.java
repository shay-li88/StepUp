package com.example.stepup;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.activity.EdgeToEdge;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;

public class CardioActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHIIT, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "";
    private FirebaseFirestore db;
    private static final String TAG = "CardioActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cardio);

        db = FirebaseFirestore.getInstance();

        View mainView = findViewById(android.R.id.content);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        btnLight = findViewById(R.id.btnLight);
        btnModerate = findViewById(R.id.btnModerate);
        btnHIIT = findViewById(R.id.btnHIIT);
        btnGo = findViewById(R.id.btnGoCardio);
        timePicker = findViewById(R.id.cardioTimePicker);
        etNotes = findViewById(R.id.etCardioNotes);

        if (timePicker != null) {
            timePicker.setMinValue(5);
            timePicker.setMaxValue(120);
            timePicker.setValue(35);
        }

        setupLevelButton(btnLight);
        setupLevelButton(btnModerate);
        setupLevelButton(btnHIIT);

        if (btnGo != null) {
            btnGo.setOnClickListener(v -> publishWorkout());
        }
    }

    private void publishWorkout() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDifficulty.isEmpty()) {
            Toast.makeText(this, "Please select difficulty level", Toast.LENGTH_SHORT).show();
            return;
        }

        int time = (timePicker != null) ? timePicker.getValue() : 0;
        String notes = (etNotes != null) ? etNotes.getText().toString() : "";

        Workout newWorkout = new Workout("Cardio", selectedDifficulty, time, notes, 0.0);
        newWorkout.setUserId(currentUserId);
        newWorkout.setTimestamp(Timestamp.now());

        // שמירה לאוסף Workouts
        db.collection("Workouts").add(newWorkout)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Saved successfully!");

                    // עדכון סטטיסטיקות משתמש בצורה חכמה
                    updateUserStats(currentUserId);

                    Toast.makeText(this, "Workout saved!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MyWorkoutsActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving", e);
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserStats(String uid) {
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                // תמיד מוסיפים 3 כוכבים
                userRef.update("totalStars", FieldValue.increment(3));

                // לוגיקה לסטריק: רק אם זה האימון הראשון להיום
                Timestamp lastUpdateTS = doc.getTimestamp("lastStreakUpdate");
                Date today = new Date();

                if (lastUpdateTS == null || !isSameDay(lastUpdateTS.toDate(), today)) {
                    userRef.update(
                            "streak", FieldValue.increment(1),
                            "lastStreakUpdate", new Timestamp(today)
                    );
                    Log.d("Points", "Cardio: Streak incremented!");
                } else {
                    Log.d("Points", "Cardio: Already trained today, streak unchanged.");
                }
            }
        }).addOnFailureListener(e -> Log.e("Points", "Error updating user stats", e));
    }

    private boolean isSameDay(Date d1, Date d2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(d1);
        cal2.setTime(d2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private void setupLevelButton(Button btn) {
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            resetButtons();
            selectButton(btn);
            selectedDifficulty = btn.getText().toString();
        });
    }

    private void selectButton(Button btn) {
        btn.setBackgroundResource(R.drawable.cardio_btn_selected);
        btn.setTextColor(Color.WHITE);
    }

    private void resetButtons() {
        Button[] btns = {btnLight, btnModerate, btnHIIT};
        for (Button b : btns) {
            if (b != null) {
                b.setBackgroundResource(android.R.color.transparent);
                b.setTextColor(Color.parseColor("#C2185B"));
            }
        }
    }
}