package com.example.stepup.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * מנהל הפרופיל (ProfileManager):
 * מחלקת עזר (Utility) האחראית על עדכון וסנכרון נתוני המשתמש הקיימים במערכת (לאחר שלב ההרשמה).
 * המחלקה מטפלת בשני דברים מרכזיים: עדכון נתונים כלליים (כמו ב-EditProfileActivity) וסנכרון אטומי
 * בין העלאת תמונת פרופיל חדשה לשרת האחסון (Supabase) לבין עדכון ה-URL וחותמת הזמן ב-Firestore.
 */
public class ProfileManager {
    private static final String TAG = "ProfileManager";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final String userId;

    public ProfileManager() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        // שליפת ה-UID של המשתמש הנוכחי במידה והוא מחובר למערכת
        this.userId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * ממשק Callback מותאם אישית לעדכון ה-UI על הצלחה או כישלון של הפעולות האסינכרוניות
     */
    public interface OnProfileUpdateListener {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    /**
     * פונקציה אסינכרונית דו-שלבית (Sync Process):
     * 1. מעלה את קובץ התמונה הפיזי ל-Bucket ייעודי בשרת האחסון של Supabase Storage.
     * 2. רק לאחר שההעלאה מצליחה והשרת מחזיר URL ציבורי, מתבצע מעבר אוטומטי לעדכון ה-Database ב-Firestore.
     */
    public void uploadAndSyncProfilePicture(File imageFile, OnProfileUpdateListener listener) {
        if (userId == null || imageFile == null) {
            if (listener != null) listener.onFailure("User not logged in or file is null");
            return;
        }

        // הגדרת נתיב הקובץ והשם שלו ב-Supabase (דריסת התמונה הקודמת של אותו משתמש באמצעות ה-UID שלו)
        String filename = "profile_pics/" + userId + ".jpg";

        SupabaseStorageHelper.uploadPicture(imageFile, filename, (success, url, error) -> {
            if (success) {
                // שלב ב': לאחר הצלחה בהעלאה ל-Storage, נעדכן את ה-Database (Firestore) עם ה-URL שקיבלנו
                updateFirestoreImageUrl(url, listener);
            } else {
                if (listener != null) listener.onFailure(error);
            }
        });
    }

    /**
     * עדכון ה-URL וחותמת הזמן בתוך מסמך המשתמש הספציפי באוסף "users" ב-Firestore
     */
    private void updateFirestoreImageUrl(String url, OnProfileUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", url); // שמירת הקישור לתמונה החדשה

        // קריטי: שמירת חותמת הזמן הנוכחית במילישניות.
        // שדה זה משרת את מנגנון ה-Signature של ספריית Glide במסך ה-ProfileActivity כדי לאלץ רענון של ה-Cache.
        updates.put("lastImageUpdate", System.currentTimeMillis());

        db.collection("users").document(userId)
                .update(updates) // עדכון חלקי (רק השדות המשתנים) מבלי לדרוס את שאר נתוני המשתמש
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore updated successfully with new URL");
                    if (listener != null) listener.onSuccess(url); // דיווח חיובי ל-UI
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * פונקציה גנרית המקבלת מפה (Map) של שדות וערכים ומעדכנת אותם ישירות במסמך המשתמש ב-Firestore.
     * משמשת לעדכון נתונים פיזיולוגיים (כמו משקל, גובה, גיל) מתוך מסך עריכת הפרופיל.
     */
    public void updateUserData(Map<String, Object> data, OnProfileUpdateListener listener) {
        if (userId == null) return;

        db.collection("users").document(userId)
                .update(data) // פקודת שאילתת עדכון ב-Firestore
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess(null); // הצלחה - הנתונים עודכנו
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }
}