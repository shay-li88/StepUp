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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CardioActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHIIT, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cardio);

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
        String type = "Cardio";
        String diff = selectedDifficulty;
        int time = (timePicker != null) ? timePicker.getValue() : 0;
        String notes = (etNotes != null) ? etNotes.getText().toString() : "";
        double distance = 0.0;

        if (diff.isEmpty()) {
            Toast.makeText(this, "Please select difficulty level", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. יצירת האובייקט
        Workout newWorkout = new Workout(type, diff, time, notes, distance);

        // 2. עדכון ה-userId (קריטי להופעה ב-MyWorkouts)
        newWorkout.setUserId(currentUserId);

        // 3. שמירה ל-Firestore לאוסף "Workouts" (אותיות גדולות)
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // בתוך פונקציית השמירה
        db.collection("Workouts").add(newWorkout) // W גדולה
                .addOnSuccessListener(documentReference -> {
                    Log.d("CardioActivity", "Saved successfully!");
                    Toast.makeText(this, "Workout saved!", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(this, MyWorkoutsActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("CardioActivity", "Error saving", e);
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                });
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