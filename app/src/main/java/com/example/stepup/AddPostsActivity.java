package com.example.stepup;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.List;

public class AddPostsActivity extends AppCompatActivity {

    private EditText etTitle, etContent;
    private ImageView ivSelectedImage;
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

        db = FirebaseFirestore.getInstance();
        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        ivSelectedImage = findViewById(R.id.ivSelectedImage); // הוסיפי ל-XML
        Button btnPost = findViewById(R.id.btnPost);
        Button btnAddImage = findViewById(R.id.btnAddImage); // כפתור להוספת תמונה

        // הוספת תמונה
        btnAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // לוגיקת כפתורי האימונים - כל כפתור פותח רשימה מסוננת
        findViewById(R.id.btnRunning).setOnClickListener(v -> showWorkoutSelectionDialog("Running"));
        findViewById(R.id.btnStrength).setOnClickListener(v -> showWorkoutSelectionDialog("Strength"));
        findViewById(R.id.btnCardio).setOnClickListener(v -> showWorkoutSelectionDialog("Cardio"));
        findViewById(R.id.btnPilates).setOnClickListener(v -> showWorkoutSelectionDialog("Pilates"));

        btnPost.setOnClickListener(v -> publishPost());
    }

    private void showWorkoutSelectionDialog(String type) {
        // שליפת אימונים רק מהסוג שנלחץ
        db.collection("Workouts")
                .whereEqualTo("type", type)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No " + type + " workouts found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Workout> workouts = queryDocumentSnapshots.toObjects(Workout.class);
                    String[] options = new String[workouts.size()];
                    for (int i = 0; i < workouts.size(); i++) {
                        Workout w = workouts.get(i);
                        options[i] = w.getType() + " - " + w.getTime() + " min (" + w.getDifficulty() + ")";
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Select a " + type + " workout to share")
                            .setItems(options, (dialog, which) -> {
                                attachedWorkout = workouts.get(which);
                                Toast.makeText(this, "Attached: " + options[which], Toast.LENGTH_SHORT).show();
                            })
                            .show();
                });
    }

    private void publishPost() {
        String title = etTitle.getText().toString();
        String content = etContent.getText().toString();
        String userId = FirebaseAuth.getInstance().getUid();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in title and content", Toast.LENGTH_SHORT).show();
            return;
        }

        String wType = (attachedWorkout != null) ? attachedWorkout.getType() : null;
        String wDetails = (attachedWorkout != null) ? attachedWorkout.getTime() + " min | " + attachedWorkout.getDifficulty() : null;

        // יצירת הפוסט (אם יש תמונה, בחיים האמיתיים תצטרכי להעלות אותה ל-Storage קודם ולקבל URL)
        Post newPost = new Post(userId, "User Name", content, wType, wDetails);
        // הערה: כדאי להוסיף שדה title למודל Post אם הוא קריטי לך

        db.collection("Posts").add(newPost)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Posted!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}