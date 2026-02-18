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

import com.google.firebase.firestore.FirebaseFirestore;

public class StrengthActivity extends AppCompatActivity {

    private Button btnLight, btnModerate, btnHeavy, btnUpper, btnLower, btnFull, btnGo;
    private NumberPicker timePicker;
    private EditText etNotes;
    private String selectedDifficulty = "Light";
    private String selectedType = "Full Body";
    private static final String TAG = "StrengthActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_strength);

        // טיפול במרווחי מערכת
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- אתחול רכיבים ---
        btnLight = findViewById(R.id.btnLight);
        btnModerate = findViewById(R.id.btnModerate);
        btnHeavy = findViewById(R.id.btnHeavy);
        btnUpper = findViewById(R.id.btnUpper);
        btnLower = findViewById(R.id.btnLower);
        btnFull = findViewById(R.id.btnFull);
        btnGo = findViewById(R.id.btnGoStrength);
        timePicker = findViewById(R.id.strengthTimePicker);
        etNotes = findViewById(R.id.etStrengthNotes);

        if (timePicker != null) {
            timePicker.setMinValue(5);
            timePicker.setMaxValue(120);
            timePicker.setValue(45);
        }

        // הגדרת כפתורי רמת קושי
        setupSelection(new Button[]{btnLight, btnModerate, btnHeavy}, btn -> selectedDifficulty = btn.getText().toString());

        // הגדרת כפתורי סוג אימון
        setupSelection(new Button[]{btnUpper, btnLower, btnFull}, btn -> selectedType = btn.getText().toString());

        // --- לוגיקת שמירה ל-Firestore ומעבר מסך ---
        btnGo.setOnClickListener(v -> {
            String fullType = "Strength" + selectedType;
            String diff = selectedDifficulty;
            int time = timePicker.getValue();
            String notes = etNotes.getText().toString();

            // 1. יצירת אובייקט האימון
            Workout newWorkout = new Workout(fullType, diff, time, notes);

            // 2. שמירה ל-Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("Workouts").add(newWorkout)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Workout saved with ID: " + documentReference.getId());
                        Toast.makeText(StrengthActivity.this, "Strength workout saved!", Toast.LENGTH_SHORT).show();

                        // 3. מעבר למסך הסיכום רק לאחר הצלחה
                        Intent intent = new Intent(StrengthActivity.this, WorkoutsActivity.class);
                        intent.putExtra("type", fullType);
                        intent.putExtra("difficulty", diff);
                        intent.putExtra("time", time);
                        intent.putExtra("notes", notes);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding document", e);
                        Toast.makeText(StrengthActivity.this, "Error saving: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
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
            b.setBackgroundResource(android.R.color.transparent);
            b.setTextColor(Color.parseColor("#4A148C")); // צבע סגול כהה
        }
        selected.setBackgroundResource(R.drawable.strength_selected); // ודאי שקיים drawable כזה
        selected.setTextColor(Color.WHITE);
    }

    interface OnSelectionListener { void onSelected(Button b); }
}