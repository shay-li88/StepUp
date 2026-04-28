package com.example.stepup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etAge, etHeight, etWeight;
    private Button btnSave;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            initViews();
            loadCurrentUserData(); // הוספת טעינת נתונים קיימים
        } else {
            finish();
        }
    }

    private void initViews() {
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        btnSave = findViewById(R.id.btnSaveDetails);

        btnSave.setOnClickListener(v -> saveUserData());
    }

    /**
     * טוען את הנתונים הנוכחיים מה-Firestore כדי שהשדות לא יהיו ריקים
     */
    private void loadCurrentUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("age")) etAge.setText(String.valueOf(documentSnapshot.getLong("age")));
                        if (documentSnapshot.contains("height")) etHeight.setText(String.valueOf(documentSnapshot.getDouble("height")));
                        if (documentSnapshot.contains("weight")) etWeight.setText(String.valueOf(documentSnapshot.getDouble("weight")));
                    }
                });
    }

    private void saveUserData() {
        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();

        if (ageStr.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int age = Integer.parseInt(ageStr);
            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);

            // חישוב BMI
            double heightInMeters = height / 100;
            double bmi = weight / (heightInMeters * heightInMeters);

            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("age", age);
            userUpdate.put("height", height);
            userUpdate.put("weight", weight);
            userUpdate.put("bmi", bmi);

            // עדכון חותמת זמן כללית לעדכון הפרופיל
            userUpdate.put("lastUpdate", System.currentTimeMillis());

            db.collection("users").document(userId)
                    .set(userUpdate, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid numbers", Toast.LENGTH_SHORT).show();
        }
    }
}