package com.example.stepup;

import android.app.ProgressDialog;
import android.content.Intent;
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

/**
 * מסך יצירת פוסט חדש (AddPostsActivity):
 * מאפשר למשתמש לכתוב פוסט עצמאי, לצרף תמונה (שעולה לשרת Supabase),
 * או לקבל נתוני אימון מוכנים מתוך כפתור שיתוף (Shared Mode) ולפרסם הכל לתוך Firestore.
 */
public class AddPostsActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private ImageView ivSelectedImage;
    private FirebaseFirestore db;

    private UserImageSelector imageSelector; // רכיב עזר מותאם אישית שפתחנו לבחירת תמונה מהגלריה/מצלמה
    private File selectedImageFile; // קובץ התמונה הפיזי שנשמר זמנית במכשיר לאחר הבחירה

    // משתני עזר לניהול מצב שבו הגענו דרך כפתור "שיתוף אימון"
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

        // אתחול רכיב בחירת התמונה והגדרת קולבק (Callback) שמגיב ברגע שהמשתמש בחר תמונה בהצלחה
        imageSelector = new UserImageSelector(this, ivSelectedImage, new OnResultCallback() {
            @Override
            public void onResult(boolean success, String url, String error) {
                if (success) {
                    // יצירת קובץ מהתמונה שנבחרה כדי שנוכל להעלות אותו פיזית לענן מאוחר יותר
                    selectedImageFile = imageSelector.createImageFile();
                    ivSelectedImage.setVisibility(View.VISIBLE); // הצגת התמונה במסך כ-Preview למשתמש
                } else {
                    Log.e(TAG, "Image selection failed: " + error);
                }
            }
        });

        // לחיצה על כפתור הוספת התמונה (פותח דיאלוג לבחירה בין מצלמה לגלריה)
        findViewById(R.id.btnAddImage).setOnClickListener(v -> imageSelector.showImageSourceDialog());

        // --- קבלת נתונים מאימון ששותף (Shared Mode) ---
        // בודק האם ה-Intent שהעביר אותנו לכאן מכיל את הדגל "isShared".
        // אם כן, אנו שולפים את הנתונים וממלאים אוטומטית את שדות הטקסט.
        if (getIntent().getBooleanExtra("isShared", false)) {
            isSharedMode = true;
            etTitle.setText(getIntent().getStringExtra("sharedTitle"));
            etContent.setText(getIntent().getStringExtra("sharedContent"));
            sharedWorkoutType = getIntent().getStringExtra("workoutType");
            sharedWorkoutDetails = getIntent().getStringExtra("workoutDetails");
        }

        // כפתור הפרסום הסופי של הפוסט
        findViewById(R.id.btnPost).setOnClickListener(v -> handlePostSubmission());
    }

    /**
     * פונקציה ראשונית הבודקת את תקינות הקלט (ולידציה) ומחליטה על נתיב ההעלאה
     */
    private void handlePostSubmission() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // זרימת עבודה מותנית (Conditional Workflow):
        // אם המשתמש בחר תמונה, קודם כל חובה להעלות את הקובץ ל-Supabase ורק אז לשמור ב-Firestore.
        if (selectedImageFile != null) {
            uploadImageAndPublish(title, content);
        } else {
            // אם אין תמונה, מפרסמים ישירות את הטקסט ל-Firestore (ומעבירים null בקישור התמונה)
            publishToFirestore(title, content, null);
        }
    }

    /**
     * שלב א' (כשיש תמונה): העלאת קובץ התמונה ל-Storage של Supabase
     */
    private void uploadImageAndPublish(String title, String content) {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("מפרסם פוסט עם תמונה");
        pd.setCancelable(false); // מונע מהמשתמש לבטל את הלחיצה באמצע התהליך
        pd.show();

        // אבטחה ומניעת דריסה: יצירת נתיב ושם ייחודי לתמונה בענן בעזרת מזהה אקראי (UUID)
        String uniqueFileName = "post_images/" + UUID.randomUUID().toString() + ".jpg";

        // פנייה לקלאס העזר של Supabase להעלאת קבצים בינאריים
        SupabaseStorageHelper.uploadPicture(selectedImageFile, uniqueFileName, (success, url, error) -> {
            // חזרה ל-Main Thread (חוט הממשק הראשי) כדי לבצע שינויי UI בבטחה
            runOnUiThread(() -> {
                pd.dismiss(); // סגירת דיאלוג הטעינה
                if (success) {
                    // הצלחה! כעת יש לנו את ה-URL הציבורי של התמונה מ-Supabase, נעבור לשלב ב' (רישום ב-Firestore)
                    publishToFirestore(title, content, url);
                } else {
                    Toast.makeText(this, "שגיאה בהעלאת התמונה: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * שלב ב' (או שלב א' ללא תמונה): יצירת מסמך הפוסט ושמירתו ב-Cloud Firestore
     */
    private void publishToFirestore(String title, String content, String imageUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        // חילוץ שם התצוגה של המשתמש. אם לא קיים - חותכים את החלק הראשון של כתובת המייל שלו
        String userName = user.getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = user.getEmail() != null ? user.getEmail().split("@")[0] : "Anonymous";
        }

        // בניית מפת הנתונים (Map) שתישמר כמסמך ב-Database
        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", user.getUid());
        postData.put("userName", userName);
        postData.put("title", title);
        postData.put("content", content);
        postData.put("imageUrl", imageUrl); // קישור התמונה הציבורי מ-Supabase (או null)
        postData.put("timestamp", com.google.firebase.Timestamp.now()); // זמן יצירת הפוסט הנוכחי
        postData.put("commentCount", 0); // אתחול כמות תגובות ל-0
        postData.put("likedBy", new java.util.ArrayList<String>()); // אתחול מערך לייקים ריק

        // בדיקה האם הפוסט נולד מתוך שיתוף אימון - כדי שהפיד ידע לצבוע ולהוסיף לו תג (Badge) מותאם
        if (isSharedMode) {
            postData.put("hasWorkout", true);
            postData.put("workoutType", sharedWorkoutType);
            postData.put("workoutDetails", sharedWorkoutDetails);
        } else {
            postData.put("hasWorkout", false);
        }

        // הוספת המסמך בפועל לאוסף "posts" בתוך Firestore (.add מייצר אוטומטית מזהה מסמך רנדומלי)
        db.collection("posts").add(postData)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Post shared successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // סגירת המסך הנוכחי וחזרה למסך הפיד בצורה חלקה
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding post: " + e.getMessage());
                    Toast.makeText(this, "Error sharing post", Toast.LENGTH_SHORT).show();
                });
    }
}