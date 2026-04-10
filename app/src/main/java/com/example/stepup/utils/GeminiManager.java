package com.example.stepup.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerativeBackend;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.Executor;

public class GeminiManager {
    private static GeminiManager instance;

    // שימוש בגרסה היציבה והמהירה ביותר למובייל
    private static final String modelVersion = "gemini-1.5-flash";
    private static final String TAG = "GeminiManager";

    private GeminiManager() {}

    public static GeminiManager getInstance() {
        if (null == instance) {
            instance = new GeminiManager();
        }
        return instance;
    }

    // פונקציה לשליחת טקסט בלבד (למשל: "תן לי טיפ לאימון")
    public void sendText(String promptStr, Context context, GeminiCallback callback) {
        Log.d(TAG, "sendText: start");
        send(promptStr, null, null, null, context, callback);
    }

    // פונקציה לשליחת תמונה וטקסט (למשל: "מה רואים בתמונת האימון הזו?")
    public void sendImageAndText(Bitmap bitmap, String promptStr, Context context, GeminiCallback callback) {
        Log.d(TAG, "sendImageAndText: start");
        send(promptStr, bitmap, null, null, context, callback);
    }

    // הפונקציה המרכזית שמבצעת את הקריאה ל-Firebase AI
    private void send(String promptStr, Bitmap bitmap, byte[] bytes, String mimeType, Context context, GeminiCallback callback) {
        Log.d(TAG, "send: initializing AI model");

        // הגדרת המודל דרך Firebase AI
        GenerativeModel ai = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(modelVersion);
        GenerativeModelFutures model = GenerativeModelFutures.from(ai);

        Content.Builder builder = new Content.Builder();
        if (bitmap != null) builder.addImage(bitmap);
        if (bytes != null) builder.addInlineData(bytes, mimeType);

        Content prompt = builder.addText(promptStr).build();

        // הרצה על ה-Main Thread כדי שנוכל לעדכן את ה-UI בקלות
        Executor executor = ContextCompat.getMainExecutor(context);
        ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                Log.d(TAG, "onSuccess: Response received");
                callback.onSuccess(result.getText());
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "onFailure: " + t.getMessage());
                callback.onError(t);
            }
        }, executor);
    }

    // ממשק (Interface) לקבלת התשובה מה-AI
    public interface GeminiCallback {
        void onSuccess(String result);
        void onError(Throwable error);
    }
}