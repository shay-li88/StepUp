package com.example.stepup.utils;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * מנהל הרישום (RegistrationManager):
 * מחלקת עזר (Utility) המנהלת את תהליך ההרשמה של משתמש חדש באפליקציה בצורה אטומית ומדורגת (State Machine).
 * התהליך מורכב מ-5 שלבים עוקבים, ומבטיח שאם אחד מהשלבים נכשל - מופעל מנגנון פיצוי (Rollback) המוחק את המשתמש כדי למנוע נתונים יתומים במערכת.
 */
public class RegistrationManager {
    private static final String TAG = "RegistrationManager";

    // הגדרת קבועים (Constants) המייצגים את שלבי תהליך ההרשמה
    private static final int REGISTRATION_PHASE_VALIDATE_USER_INFO = 0;
    private static final int REGISTRATION_PHASE_CREATE_USER = 1;
    private static final int REGISTRATION_PHASE_UPLOAD_PIC = 2;
    private static final int REGISTRATION_PHASE_UPLOAD_DATA = 3;
    private static final int REGISTRATION_PHASE_DONE = 4;

    private int registrationPhase; // משתנה המצב הנוכחי של המכונה

    FirebaseAuth auth;
    FirebaseFirestore db; // הוספנו את Firestore

    File imageFile;
    String userId;
    String email;
    String password;
    String nickname;
    int age;
    Activity activity;

    OnResultCallback onResultCallback;

    public RegistrationManager(Activity activity) {
        Log.d(TAG, "RegistrationManager: started");
        this.activity = activity;
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // אתחול Firestore
        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO; // התחלה משלב 0
    }

    /**
     * נקודת הכניסה הראשית להפעלת תהליך הרישום האסינכרוני
     */
    public void startRegistration(String email, String password, File imageFile, String nickname, int age, OnResultCallback onResultCallback) {
        this.onResultCallback = onResultCallback;
        this.email = email;
        this.password = password;
        this.imageFile = imageFile;
        this.nickname = nickname;
        this.age = age;
        executeNextPhase(); // התחלת ביצוע השלב הראשון
    }

    /**
     * ממשק (Interface) המשמש כ-Callback להחזרת תוצאת התהליך כולו למסך ה-UI (למשל ל-RegisterActivity)
     */
    public interface OnResultCallback {
        void onResult(boolean success, String message);
    }

    /**
     * פונקציה המקודמת את המצב הנוכחי בשלב אחד קדימה ומפעילה אותו
     */
    private void phaseDone() {
        registrationPhase++;
        executeNextPhase();
    }

    /**
     * מנגנון הפיצוי (Rollback / Transaction):
     * במידה ושלב כלשהו נכשל (למשל העלאת התמונה או כתיבה ל-Firestore), הפונקציה מוחקת את חשבון ה-Auth
     * שנוצר בשלב 1, ומבצעת Sign Out. זה מונע מצב של "משתמשים יתומים" שקיימים ב-Auth אך אין להם ייצוג ב-Database.
     */
    private void phaseFailed(String message) {
        Log.e(TAG, "phaseFailed: registration failed: message: " + message);
        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO; // איפוס המכונה
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.delete(); // מחיקת המשתמש מה-Authentication (פעולת ה-Rollback)
        }
        auth.signOut();
        onResultCallback.onResult(false, message); // דיווח למסך על הכישלון
    }

    /**
     * נתב השלבים (The State Machine Engine):
     * פונקציה המנווטת ומפעילה את הלוגיקה המתאימה בהתאם לשלב הנוכחי בתהליך
     */
    private void executeNextPhase() {
        Log.d(TAG, "executeNextPhase: executing phase: " + registrationPhase);
        if(registrationPhase == REGISTRATION_PHASE_VALIDATE_USER_INFO) validateUserInfo();
        else if(registrationPhase == REGISTRATION_PHASE_CREATE_USER) createUser();
        else if(registrationPhase == REGISTRATION_PHASE_UPLOAD_PIC) uploadProfilePictureToSupabase();
        else if(registrationPhase == REGISTRATION_PHASE_UPLOAD_DATA) saveUserToFirestore();
        else if(registrationPhase == REGISTRATION_PHASE_DONE) onResultCallback.onResult(true, "Registration successful!");
    }

    /**
     * שלב 0: בדיקת תקינות ראשונית לקלטים קריטיים (Validation מקומי)
     */
    private void validateUserInfo() {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            phaseFailed("Please fill in all fields");
            return;
        }
        phaseDone();
    }

    /**
     * שלב 1: יצירת המשתמש בתוך מערכת ה-Firebase Authentication ועדכון ה-DisplayName (הכינוי)
     */
    private void createUser() {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            userId = user.getUid(); // שמירת ה-UID הייחודי שנוצר מה-Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nickname)
                                    .build();
                            user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> phaseDone());
                        } else {
                            phaseFailed("user is null");
                        }
                    } else {
                        phaseFailed(task.getException() != null ? task.getException().getMessage() : "Unknown error");
                    }
                });
    }

    /**
     * שלב 2: העלאת תמונת הפרופיל לשרת האחסון החיצוני (Supabase Storage) באמצעות מחלקת עזר
     */
    private void uploadProfilePictureToSupabase() {
        if (imageFile == null) {
            phaseDone(); // אם המשתמש לא בחר תמונה, מדלגים לשלב הבא בצורה בטוחה
            return;
        }
        String filename = "images/profile-pics/" + userId + ".jpg";
        SupabaseStorageHelper.uploadPicture(imageFile, filename, (success, url, error) -> {
            if (success) phaseDone();
            else phaseFailed("Failed to upload profile picture: " + error);
        });
    }

    /**
     * שלב 3: התיקון המרכזי - בניית מסמך משתמש חדש והזרקתו ל-Firestore תחת אוסף "users".
     * כאן אנו מאתחלים את כל שדות ברירת המחדל הנדרשים לאפליקציה (סטריק, כוכבים, מדדים פיזיולוגיים וכו').
     */
    private void saveUserToFirestore() {
        Log.d(TAG, "saveUserToFirestore: Saving initial data for " + userId);

        // יצירת מפה (Map) המייצגת את שדות האובייקט בתוך ה-Document ב-Database
        Map<String, Object> user = new HashMap<>();
        user.put("name", nickname);       // השם האמיתי מההרשמה
        user.put("email", email);
        user.put("streak", 0);            // אתחול סטריק ל-0
        user.put("totalStars", 0);        // אתחול כוכבים ל-0
        user.put("totalWorkouts", 0);     // אתחול אימונים ל-0
        user.put("logs", 0);              // אתחול לוגים ל-0
        user.put("age", age);             // גיל ראשוני שהוזן בהרשמה
        user.put("height", 0.0);          // אתחול מדדים פיזיולוגיים ל-0 (יוזנו בהמשך ב-EditProfile)
        user.put("weight", 0.0);
        user.put("bmi", 0.0);

        //שאילתה
        db.collection("users").document(userId)
                .set(user) // יצירת המסמך באופן אטומי עם מפתח ה-UID של ה-Auth
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Firestore data saved successfully");
                    phaseDone(); // מעבר לשלב הסיום החיובי
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore save failed", e);
                    phaseFailed("Failed to save user data to Firestore");
                });
    }
}