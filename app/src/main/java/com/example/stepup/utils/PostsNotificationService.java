package com.example.stepup.utils; // ודאי שזה תואם לשם החבילה שלך

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
import com.example.stepup.FeedActivity; // שינוי שם הפקג' ל-StepUp
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
    private static final String TAG = "PostsNotificationService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mAfterFirstDBLoad = false;
        createPostNotificationChannel();
        // הודעת פתיחה כדי שנדע שהשירות עובד ברקע
        sendNotification("StepUp is active", "Looking for new posts...", 1, true);
        listenToChangesInPosts();
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void listenToChangesInPosts() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        // האזנה לאוסף הפוסטים שלך ב-Firestore
        firestore.collection("posts")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) return;
                        if (!mAfterFirstDBLoad) {
                            mAfterFirstDBLoad = true;
                            return;
                        }
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                // כשנוסף פוסט חדש, נשלח התראה
                                sendPostNotification(dc.getDocument().getData(), dc.getDocument().getId().hashCode());
                            }
                        }
                    }
                });
    }

    private void sendPostNotification(Map<String, Object> post, int notificationId) {
        // התאמה לשדות ב-Firestore שלך (userName ו-description)
        String userName = post.get("userName") != null ? post.get("userName").toString() : "Someone";
        String description = post.get("description") != null ? post.get("description").toString() : "posted something new!";

        sendNotification("New Post from " + userName, description, notificationId, false);
    }

    private void sendNotification(String title, String content, int notificationId, boolean startForeground) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null || !notificationManager.areNotificationsEnabled()) return;

        Intent resultIntent = new Intent(getApplicationContext(), FeedActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, POST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // ודאי שיש לך אייקון כזה
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        Notification notification = builder.build();
        if (startForeground) {
            startForeground(notificationId, notification);
        } else {
            notificationManager.notify(notificationId, notification);
        }
    }

    private void createPostNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(POST_CHANNEL_ID, "New Post Channel", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}
