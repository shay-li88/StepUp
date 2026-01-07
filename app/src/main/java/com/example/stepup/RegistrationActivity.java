package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // קישור לרכיבים לפי ה-IDs ב-XML שלך
        emailEditText = findViewById(R.id.etemail);
        passwordEditText = findViewById(R.id.etPassword);
        Button registerButton = findViewById(R.id.btn_register);
        TextView backToLogin = findViewById(R.id.link_login);

        registerButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String pass = passwordEditText.getText().toString().trim();
            String name = email; // משתמשים באימייל ככינוי זמני כי אין שדה ניקניימ

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // תיקון הקריאה לפי הסדר שה-Manager דורש:
            // 1. אימייל, 2. סיסמה, 3. null (במקום קובץ), 4. שם, 5. מספר (0), 6. קולבאק
            new RegistrationManager(this).startRegistration(
                    email,
                    pass,
                    null,   // הפרמטר השלישי חייב להיות File (או null אם אין תמונה)
                    name,   // הפרמטר הרביעי הוא ה-String של השם
                    0,      // הפרמטר החמישי הוא ה-int
                    (success, message) -> { // הפרמטר השישי הוא ה-Callback
                        if (success) {
                            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // חזרה למסך התחברות
        backToLogin.setOnClickListener(v -> finish());
    }
}