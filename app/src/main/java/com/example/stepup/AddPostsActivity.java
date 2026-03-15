package com.example.stepup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
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

        // --- קבלת נתונים משותפים מה-Adapter ---
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
        String userId = FirebaseAuth.getInstance().getUid();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) userName = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", userId);
        postData.put("userName", userName);
        postData.put("title", title);
        postData.put("content", content);
        postData.put("timestamp", com.google.firebase.Timestamp.now());
        postData.put("commentCount", 0);

        // אם זה שיתוף אימון, נשמור את פרטי האימון בפוסט
        if (isSharedMode) {
            postData.put("hasWorkout", true);
            postData.put("workoutType", sharedWorkoutType);
            postData.put("workoutDetails", sharedWorkoutDetails);
        } else {
            postData.put("hasWorkout", false);
        }

        db.collection("Posts").add(postData)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Post shared!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error posting", Toast.LENGTH_SHORT).show());
    }
}