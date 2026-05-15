package com.example.stepup;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.stepup.utils.OnResultCallback;
import com.example.stepup.utils.SupabaseStorageHelper;
import com.example.stepup.utils.UserImageSelector;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPostsActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private ImageView ivSelectedImage;
    private FirebaseFirestore db;

    private UserImageSelector imageSelector;
    private File selectedImageFile; // הקובץ שנבחר מהגלריה/מצלמה

    private boolean isSharedMode = false;
    private String sharedWorkoutType, sharedWorkoutDetails;

    private static final String TAG = "AddPostsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_posts);

        db = FirebaseFirestore.getInstance();
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);

        // אתחול רכיב בחירת התמונה
        imageSelector = new UserImageSelector(this, ivSelectedImage, new OnResultCallback() {
            @Override
            public void onResult(boolean success, String url, String error) {
                if (success) {
                    // יצירת קובץ מהתמונה שנבחרה כדי שנוכל להעלות אותו אחר כך
                    selectedImageFile = imageSelector.createImageFile();
                    ivSelectedImage.setVisibility(View.VISIBLE); // הצגת התמונה ב-Preview
                } else {
                    Log.e(TAG, "Image selection failed: " + error);
                }
            }
        });

        // לחיצה על כפתור המצלמה ב-Layout
        findViewById(R.id.btnAddImage).setOnClickListener(v -> imageSelector.showImageSourceDialog());

        // קבלת נתונים אם הגענו משיתוף אימון (Shared Mode)
        if (getIntent().getBooleanExtra("isShared", false)) {
            isSharedMode = true;
            etTitle.setText(getIntent().getStringExtra("sharedTitle"));
            etContent.setText(getIntent().getStringExtra("sharedContent"));
            sharedWorkoutType = getIntent().getStringExtra("workoutType");
            sharedWorkoutDetails = getIntent().getStringExtra("workoutDetails");
        }

        // כפתור הפרסום
        findViewById(R.id.btnPost).setOnClickListener(v -> handlePostSubmission());
    }

    private void handlePostSubmission() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // אם המשתמש בחר תמונה, קודם נעלה אותה ל-Supabase
        if (selectedImageFile != null) {
            uploadImageAndPublish(title, content);
        } else {
            // אם אין תמונה, מפרסמים ישירות ל-Firestore
            publishToFirestore(title, content, null);
        }
    }

    private void uploadImageAndPublish(String title, String content) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מפרסם פוסט עם תמונה");
        pd.setCancelable(false);
        pd.show();

        // יצירת שם ייחודי לתמונה כדי למנוע דריסת קבצים
        String uniqueFileName = "post_images/" + UUID.randomUUID().toString() + ".jpg";

        SupabaseStorageHelper.uploadPicture(selectedImageFile, uniqueFileName, (success, url, error) -> {
            runOnUiThread(() -> {
                pd.dismiss();
                if (success) {
                    publishToFirestore(title, content, url);
                } else {
                    Toast.makeText(this, "שגיאה בהעלאת התמונה: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void publishToFirestore(String title, String content, String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userName = user.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = user.getEmail() != null ? user.getEmail().split("@")[0] : "Anonymous";
        }

        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", user.getUid());
        postData.put("userName", userName);
        postData.put("title", title);
        postData.put("content", content);
        postData.put("imageUrl", imageUrl); // ה-URL מ-Supabase (יהיה null אם אין תמונה)
        postData.put("timestamp", com.google.firebase.Timestamp.now());
        postData.put("commentCount", 0);
        postData.put("likedBy", new java.util.ArrayList<String>());

        // טיפול במצב שיתוף אימון
        if (isSharedMode) {
            postData.put("hasWorkout", true);
            postData.put("workoutType", sharedWorkoutType);
            postData.put("workoutDetails", sharedWorkoutDetails);
        } else {
            postData.put("hasWorkout", false);
        }

        db.collection("posts").add(postData)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding post: " + e.getMessage());
                    Toast.makeText(this, "Error sharing post", Toast.LENGTH_SHORT).show();
                });
    }
}