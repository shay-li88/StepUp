package com.example.stepup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton; // חשוב מאוד לתיקון האדום
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPostsActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private ImageView ivSelectedImage;
    private ImageButton btnAddImage; // המצלמה שביקשת
    private Uri imageUri;
    private Workout attachedWorkout = null;
    private FirebaseFirestore db;

    // לאונצ'ר לבחירת תמונה מהגלריה
    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    imageUri = result.getData().getData();
                    ivSelectedImage.setImageURI(imageUri);
                    ivSelectedImage.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_posts);

        // אתחול רכיבים
        // ודאי שהשמות כאן תואמים ל-XML שלך
        btnAddImage = findViewById(R.id.btnAddImage);
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        ivSelectedImage = findViewById(R.id.ivSelectedImage);
        Button btnPost = findViewById(R.id.btnPost);
        // לחיצה על המצלמה לפתיחת גלריה
        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // הגדרת מאזינים לכפתורי האימונים הצבעוניים
        setupWorkoutButtons();

        // כפתור הפרסום הסופי
        btnPost.setOnClickListener(v -> publishPost());
    }

    private void setupWorkoutButtons() {
        // חיבור הכפתורים מה-XML והגדרת פעולה
        View btnRunning = findViewById(R.id.btnRunning);
        if (btnRunning != null) {
            btnRunning.setOnClickListener(v -> showWorkoutSelectionDialog("Running"));
        }

        View btnStrength = findViewById(R.id.btnStrength);
        if (btnStrength != null) {
            btnStrength.setOnClickListener(v -> showWorkoutSelectionDialog("Strength"));
        }

        View btnCardio = findViewById(R.id.btnCardio);
        if (btnCardio != null) {
            btnCardio.setOnClickListener(v -> showWorkoutSelectionDialog("Cardio"));
        }

        View btnPilates = findViewById(R.id.btnPilates);
        if (btnPilates != null) {
            btnPilates.setOnClickListener(v -> showWorkoutSelectionDialog("Pilates"));
        }
    }

    private void showWorkoutSelectionDialog(String type) {
        // שליפת אימונים מה-Firestore לפי הסוג שנבחר
        db.collection("Workouts")
                .whereEqualTo("type", type)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No " + type + " workouts found in your history", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Workout> workouts = queryDocumentSnapshots.toObjects(Workout.class);
                    String[] options = new String[workouts.size()];
                    for (int i = 0; i < workouts.size(); i++) {
                        Workout w = workouts.get(i);
                        options[i] = w.getType() + " - " + w.getTime() + " min (" + w.getDifficulty() + ")";
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Attach " + type + " Workout")
                            .setItems(options, (dialog, which) -> {
                                attachedWorkout = workouts.get(which);
                                Toast.makeText(this, "Attached: " + attachedWorkout.getType(), Toast.LENGTH_SHORT).show();
                            })
                            .show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading workouts", Toast.LENGTH_SHORT).show();
    }

    private void publishPost() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String userId = FirebaseAuth.getInstance().getUid();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // הכנת הנתונים לשליחה (כולל האימון המצורף אם יש)
        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", userId);
        postData.put("userName", "User"); // כאן כדאי להביא את השם האמיתי מהפרופיל
        postData.put("title", title);
        postData.put("content", content);
        postData.put("timestamp", com.google.firebase.Timestamp.now());

        if (attachedWorkout != null) {
            postData.put("hasWorkout", true);
            postData.put("workoutType", attachedWorkout.getType());
            postData.put("workoutDetails", attachedWorkout.getTime() + " min • " + attachedWorkout.getDifficulty());
        } else {
            postData.put("hasWorkout", false);
        }

        db.collection("Posts").add(postData)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // חזרה לדף הפוסטים
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to post", Toast.LENGTH_SHORT).show());
    }
}