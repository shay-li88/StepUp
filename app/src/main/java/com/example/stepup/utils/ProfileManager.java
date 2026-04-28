package com.example.stepup.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProfileManager {
    private static final String TAG = "ProfileManager";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final String userId;

    public ProfileManager() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.userId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
    }

    /**
     * ממשק Callback לעדכון ה-UI על הצלחה או כישלון
     */
    public interface OnProfileUpdateListener {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    /**
     * מעלה תמונה ל-Supabase ומעדכן את ה-URL ב-Firestore
     */
    public void uploadAndSyncProfilePicture(File imageFile, OnProfileUpdateListener listener) {
        if (userId == null || imageFile == null) {
            if (listener != null) listener.onFailure("User not logged in or file is null");
            return;
        }

        // הגדרת נתיב הקובץ ב-Supabase (שימוש בתיקייה ייעודית)
        String filename = "profile_pics/" + userId + ".jpg";

        SupabaseStorageHelper.uploadPicture(imageFile, filename, (success, url, error) -> {
            if (success) {
                // לאחר הצלחה בהעלאה ל-Storage, נעדכן את ה-Database (Firestore)
                updateFirestoreImageUrl(url, listener);
            } else {
                if (listener != null) listener.onFailure(error);
            }
        });
    }

    /**
     * עדכון ה-URL בתוך המסמך של המשתמש ב-Firestore
     */
    private void updateFirestoreImageUrl(String url, OnProfileUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", url);
        updates.put("lastImageUpdate", System.currentTimeMillis());

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore updated successfully with new URL");
                    if (listener != null) listener.onSuccess(url);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update Firestore", e);
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }

    /**
     * פונקציה כללית לעדכון נתוני פרופיל (שם, גיל וכו')
     */
    public void updateUserData(Map<String, Object> data, OnProfileUpdateListener listener) {
        if (userId == null) return;

        db.collection("users").document(userId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null) listener.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onFailure(e.getMessage());
                });
    }
}