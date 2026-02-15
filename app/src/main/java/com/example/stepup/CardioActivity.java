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

import com.google.firebase.firestore.FirebaseFirestore;

public class CardioActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHIIT, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = ""; // התחלה ריקה כדי לוודא בחירה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cardio);

        // טיפול במרווחי מערכת
        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- אתחול רכיבים ---
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

        // הגדרת לוגיקת הכפתורים
        setupLevelButton(btnLight);
        setupLevelButton(btnModerate);
        setupLevelButton(btnHIIT);

        // --- לוגיקת שמירה ומעבר מסך (מאוחדת) ---
        if (btnGo != null) {
            btnGo.setOnClickListener(v -> {
                String type = "Cardio";
                String diff = selectedDifficulty;
                int time = (timePicker != null) ? timePicker.getValue() : 0;
                String notes = (etNotes != null) ? etNotes.getText().toString() : "";

                // בדיקה שנבחרה רמה
                if (diff.isEmpty()) {
                    Toast.makeText(CardioActivity.this, "Please select difficulty level", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 1. יצירת אובייקט האימון
                Workout newWorkout = new Workout(type, diff, time, notes);

                // 2. שמירה ל-Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("Workouts").add(newWorkout)
                        .addOnSuccessListener(documentReference -> {
                            Log.d("CardioActivity", "Saved with ID: " + documentReference.getId());
                            Toast.makeText(CardioActivity.this, "Workout saved!", Toast.LENGTH_SHORT).show();

                            // 3. מעבר למסך סיכום רק אחרי הצלחה
                            Intent intent = new Intent(CardioActivity.this, WorkoutsActivity.class);
                            intent.putExtra("type", type);
                            intent.putExtra("difficulty", diff);
                            intent.putExtra("time", time);
                            intent.putExtra("notes", notes);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CardioActivity", "Error saving", e);
                            Toast.makeText(CardioActivity.this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
        }
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