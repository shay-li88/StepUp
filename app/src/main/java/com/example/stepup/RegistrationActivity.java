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
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // בדיקות תקינות (כבר קיימות אצלך)
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password too short");
            return;
        }

        RegistrationManager registrationManager = new RegistrationManager(this);
        registrationManager.startRegistration(
                email,
                password,
                null, // קובץ תמונה (אם יש)
                username,
                0,
                (success, message) -> {
                    if (success) {
                        Toast.makeText(RegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        // מעבר למסך לוגין
                        Intent intent = new Intent(RegistrationActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish(); // סוגר את מסך ההרשמה
                    } else {
                        Toast.makeText(RegistrationActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                });
    }

}