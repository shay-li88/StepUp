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

import com.google.firebase.auth.FirebaseAuth;



public class LoginActivity extends AppCompatActivity {
    FirebaseAuth auth;
    protected EditText emailEditText;
    protected EditText passwordEditText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        emailEditText = findViewById(R.id.et_email); // וודא שה-ID תואם ל-XML שלך
        passwordEditText = findViewById(R.id.et_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        TextView registerLinkTextView = findViewById(R.id.tvSignup);


        registerLinkTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performLogin();

            }
        });

    }
    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty() || password.isEmpty()) {
            Log.w("LoginActivity", "Empty email and/or password field");
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        // Perform Firebase authentication
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.i("LoginActivity", "signInWithEmail:success");
                        startFeedActivity(true);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("LoginActivity", "signInWithEmail:failure", task.getException());

                        String errorMessage = "Authentication failed. ";

                        if (task.getException() != null) {
                            errorMessage += task.getException().getMessage();
                        }

                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void startFeedActivity(boolean sendToast) {
        if(sendToast)
            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

        // Navigate to FeedActivity
        Intent intent = new Intent(LoginActivity.this, FeedActivity.class);
        startActivity(intent);
        finish();
    }

}