package com.example.stepup.utils;

import android.app.Application;
import androidx.lifecycle.ProcessLifecycleOwner;

public class StepUpApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // אתחול ורישום של ה-Observer כדי שיאזין למעבר בין רקע לחזית
        AppLifecycleObserver appLifecycleObserver = new AppLifecycleObserver(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(appLifecycleObserver);
    }
}
