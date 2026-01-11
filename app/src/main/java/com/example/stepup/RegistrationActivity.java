package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.stepup.utils.RegistrationManager;

public class RegistrationActivity extends AppCompatActivity {
    // ב-XML ששלחת אין שדה Nickname, לכן השארתי רק אימייל וסיסמה
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText usernameEditText;
    private static final String TAG = "RegistrationActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started");
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;


        });
        usernameEditText = findViewById(R.id.etUsername);
        emailEditText = findViewById(R.id.etEmail);
        passwordEditText = findViewById(R.id.etPassword);

        Button registerButton = findViewById(R.id.btn_register);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerButtonClick();
            }
        });


        Log.d(TAG, "onCreate: done");
    }
    private void registerButtonClick() {
        Log.d(TAG, "Register button clicked");
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // 1. בדיקה אם השדות ריקים
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(RegistrationActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return; // עוצר כאן ולא ממשיך לרישום
        }

        // 2. בדיקה אם האימייל תקין (מכיל @ ונקודה במבנה נכון)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // אם האימייל לא תקין
            emailEditText.setError("Invalid email format (missing @ or .)");
            Toast.makeText(RegistrationActivity.this, "Registration failed: Invalid email", Toast.LENGTH_SHORT).show();
            return; // עוצר כאן ונשאר בדף הרישום
        }

        // 3. בדיקת אורך סיסמה (בונוס - כדאי שתהיה לפחות 6 תווים)
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return;
        }

        // רק אם הכל תקין - עוברים לרישום האמיתי
        RegistrationManager registrationManager = new RegistrationManager(RegistrationActivity.this);
        registrationManager.startRegistration(
                email,
                password,
                null,
                username,
                0,
                new RegistrationManager.OnResultCallback() {
                    @Override
                    public void onResult(boolean success, String message) {
                        if (success) {
                            // עדכון השם ב-Firebase Auth כדי שנוכל למשוך אותו בכל מקום באפליקציה
                            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                com.google.firebase.auth.UserProfileChangeRequest profileUpdates = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(username) // ה-username מה-EditText
                                        .build();

                                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                                    Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                                    startActivity(intent);
                                    finish();
                                });
                            }
                        }
                    }
                });
    }

}