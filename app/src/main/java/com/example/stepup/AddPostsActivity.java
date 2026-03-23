package com.example.stepup;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class AddPostsActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private FirebaseFirestore db;
    private boolean isSharedMode = false;
    private String sharedWorkoutType, sharedWorkoutDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_posts);

        db = FirebaseFirestore.getInstance();
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);

        // קבלת נתונים אם הגענו משיתוף אימון
        if (getIntent().getBooleanExtra("isShared", false)) {
            isSharedMode = true;
            etTitle.setText(getIntent().getStringExtra("sharedTitle"));
            etContent.setText(getIntent().getStringExtra("sharedContent"));
            sharedWorkoutType = getIntent().getStringExtra("workoutType");
            sharedWorkoutDetails = getIntent().getStringExtra("workoutDetails");
        }

        findViewById(R.id.btnPost).setOnClickListener(v -> publishPost());
    }

    private void publishPost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // קביעת שם המשתמש (Display Name או חלק מהאימייל)
        String userName = user.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            String email = user.getEmail();
            if (email != null) userName = email.split("@")[0];
            else userName = "Anonymous";
        }

        // הכנת הנתונים למשלוח
        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", user.getUid());
        postData.put("userName", userName);
        postData.put("title", title);
        postData.put("content", content);
        postData.put("timestamp", com.google.firebase.Timestamp.now());
        postData.put("commentCount", 0);

        if (isSharedMode) {
            postData.put("hasWorkout", true);
            postData.put("workoutType", sharedWorkoutType);
            postData.put("workoutDetails", sharedWorkoutDetails);
        } else {
            postData.put("hasWorkout", false);
        }

        // שימוש ב-"posts" באות קטנה להתאמה לכל שאר האפליקציה
        db.collection("posts").add(postData)
                .addOnSuccessListener(doc -> {
                    Log.d("AddPost", "Post shared successfully in 'posts' collection");
                    Toast.makeText(this, "Post shared!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("AddPost", "Error: " + e.getMessage());
                    Toast.makeText(this, "Error posting", Toast.LENGTH_SHORT).show();
                });
    }
}