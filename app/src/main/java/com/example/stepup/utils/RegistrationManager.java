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

public class RegistrationManager {
    private static final String TAG = "RegistrationManager";

    private static final int REGISTRATION_PHASE_VALIDATE_USER_INFO = 0;
    private static final int REGISTRATION_PHASE_CREATE_USER = 1;
    private static final int REGISTRATION_PHASE_UPLOAD_PIC = 2;
    private static final int REGISTRATION_PHASE_UPLOAD_DATA = 3;
    private static final int REGISTRATION_PHASE_DONE = 4;
    private int registrationPhase;
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
        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO;
    }

    public void startRegistration(String email, String password, File imageFile, String nickname, int age, OnResultCallback onResultCallback) {
        this.onResultCallback = onResultCallback;
        this.email = email;
        this.password = password;
        this.imageFile = imageFile;
        this.nickname = nickname;
        this.age = age;
        executeNextPhase();
    }

    public interface OnResultCallback {
        void onResult(boolean success, String message);
    }

    private void phaseDone() {
        registrationPhase++;
        executeNextPhase();
    }

    private void phaseFailed(String message) {
        Log.e(TAG, "phaseFailed: registration failed: message: " + message);
        registrationPhase = REGISTRATION_PHASE_VALIDATE_USER_INFO;
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            user.delete();
        }
        auth.signOut();
        onResultCallback.onResult(false, message);
    }

    private void executeNextPhase() {
        Log.d(TAG, "executeNextPhase: executing phase: " + registrationPhase);
        if(registrationPhase == REGISTRATION_PHASE_VALIDATE_USER_INFO) validateUserInfo();
        else if(registrationPhase == REGISTRATION_PHASE_CREATE_USER) createUser();
        else if(registrationPhase == REGISTRATION_PHASE_UPLOAD_PIC) uploadProfilePictureToSupabase();
        else if(registrationPhase == REGISTRATION_PHASE_UPLOAD_DATA) saveUserToFirestore();
        else if(registrationPhase == REGISTRATION_PHASE_DONE) onResultCallback.onResult(true, "Registration successful!");
    }

    private void validateUserInfo() {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            phaseFailed("Please fill in all fields");
            return;
        }
        phaseDone();
    }

    private void createUser() {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(activity, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            userId = user.getUid();
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

    private void uploadProfilePictureToSupabase() {
        if (imageFile == null) {
            phaseDone();
            return;
        }
        String filename = "images/profile-pics/" + userId + ".jpg";
        SupabaseStorageHelper.uploadPicture(imageFile, filename, (success, url, error) -> {
            if (success) phaseDone();
            else phaseFailed("Failed to upload profile picture: " + error);
        });
    }

    // התיקון המרכזי כאן - שמירת כל הנתונים שביקשת
    private void saveUserToFirestore() {
        Log.d(TAG, "saveUserToFirestore: Saving initial data for " + userId);

        Map<String, Object> user = new HashMap<>();
        user.put("name", nickname);       // השם האמיתי מההרשמה
        user.put("email", email);
        user.put("streak", 0);            // אתחול סטריק ל-0
        user.put("totalStars", 0);        // אתחול כוכבים ל-0
        user.put("totalWorkouts", 0);     // אתחול אימונים ל-0
        user.put("logs", 0);              // אתחול לוגים ל-0
        user.put("age", age);             // גיל ראשוני (0)
        user.put("height", 0.0);
        user.put("weight", 0.0);
        user.put("bmi", 0.0);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "Firestore data saved successfully");
                    phaseDone();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore save failed", e);
                    phaseFailed("Failed to save user data to Firestore");
                });
    }
}