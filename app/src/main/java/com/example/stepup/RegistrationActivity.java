package com.example.stepup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.stepup.utils.RegistrationManager;

public class RegistrationActivity extends AppCompatActivity {
    protected EditText emailEditText;
    protected EditText passwordEditText;
    protected EditText nicknameEditText;
    private static final String TAG = "RegistrationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);

        // 1. אתחול השדות - חובה לקשר ל-ID מה-XML
        emailEditText = findViewById(R.id.et_email); // וודא שה-ID תואם ל-XML שלך
        passwordEditText = findViewById(R.id.et_password);
        Button registerButton = findViewById(R.id.btn_register);

        // 2. הגדרת המאזין כבר ברגע יצירת המסך
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //registerButtonClick();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }




}