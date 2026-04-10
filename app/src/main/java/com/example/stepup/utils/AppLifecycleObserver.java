package com.example.stepup.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private final Context context;
    private static final String TAG = "AppLifecycleObserver";

    public AppLifecycleObserver(Context context) {
        // שימוש ב-Application Context למניעת זליגות זיכרון
        this.context = context.getApplicationContext();
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        // האפליקציה חזרה לקדמת המסך (Foreground)
        Log.d(TAG, "App is in FOREGROUND");

        // מפסיקים את שירות ההתראות כי המשתמש כבר בתוך האפליקציה
        Intent serviceIntent = new Intent(context, PostsNotificationService.class);
        context.stopService(serviceIntent);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // האפליקציה עברה לרקע (Background)
        Log.d(TAG, "App is in BACKGROUND");

        // מפעילים את שירות ההתראות כדי להאזין לפוסטים חדשים
        Intent serviceIntent = new Intent(context, PostsNotificationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}