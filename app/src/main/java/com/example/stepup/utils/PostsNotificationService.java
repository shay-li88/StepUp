package com.example.stepup.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.stepup.FeedActivity;
import com.example.stepup.R;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Map;

public class PostsNotificationService extends Service {
    private static final String POST_CHANNEL_ID = "POST_CHANNEL_ID";
    private boolean mAfterFirstDBLoad;
    private static final String TAG = "StepUp_Service"; // תגית ברורה ל-Logcat

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: Service started with ID: " + startId);
        mAfterFirstDBLoad = false;
        createPostNotificationChannel();

        // הודעת פתיחה כדי שנדע שהשירות עובד ברקע
        Log.d(TAG, "onStartCommand: Initializing foreground notification");
        sendNotification("StepUp is active", "Looking for new posts...", 1, true);

        listenToChangesInPosts();

        // START_STICKY אומר למערכת לנסות להפעיל את השירות מחדש אם הוא נסגר מחוסר משאבים
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Service bound");
        return null;
    }

    private void listenToChangesInPosts() {
        Log.d(TAG, "listenToChangesInPosts: Setting up Firestore listener on 'posts' collection");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("posts")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "onEvent: Firestore listener error!", e);
                            return;
                        }

                        if (snapshots == null) {
                            Log.w(TAG, "onEvent: Received null snapshots");
                            return;
                        }

                        Log.d(TAG, "onEvent: Received snapshot update. Document count: " + snapshots.size());

                        if (!mAfterFirstDBLoad) {
                            Log.i(TAG, "onEvent: Initial data loaded. Skipping notifications for existing documents.");
                            mAfterFirstDBLoad = true;
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Log.d(TAG, "onEvent: Change detected: Type = " + dc.getType());
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String docId = dc.getDocument().getId();
                                Log.i(TAG, "onEvent: New post detected! ID: " + docId);
                                sendPostNotification(dc.getDocument().getData(), docId.hashCode());
                            }
                        }
                    }
                });
    }

    private void sendPostNotification(Map<String, Object> post, int notificationId) {
        String userName = post.get("userName") != null ? post.get("userName").toString() : "Someone";
        String description = post.get("description") != null ? post.get("description").toString() : "posted something new!";

        Log.d(TAG, "sendPostNotification: Preparing notification for user: " + userName);
        sendNotification("New Post from " + userName, description, notificationId, false);
    }

    private void sendNotification(String title, String content, int notificationId, boolean startForeground) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (notificationManager == null) {
            Log.e(TAG, "sendNotification: NotificationManager is null!");
            return;
        }

        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "sendNotification: Notifications are disabled by the user");
            return;
        }

        Log.d(TAG, "sendNotification: Building notification: " + title);

        Intent resultIntent = new Intent(getApplicationContext(), FeedActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(resultIntent);

        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, POST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        Notification notification = builder.build();
        if (startForeground) {
            Log.i(TAG, "sendNotification: Starting service in foreground mode");
            startForeground(notificationId, notification);
        } else {
            Log.d(TAG, "sendNotification: Posting regular notification. ID: " + notificationId);
            notificationManager.notify(notificationId, notification);
        }
    }

    private void createPostNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "createPostNotificationChannel: Creating channel ID: " + POST_CHANNEL_ID);
            NotificationChannel channel = new NotificationChannel(POST_CHANNEL_ID, "New Post Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "onDestroy: Service is being destroyed!");
        super.onDestroy();
    }
}