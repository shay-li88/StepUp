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
import com.example.stepup.HomeActivity;
import com.example.stepup.R;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.Map;

/*רכיב Service (שירות) הרץ ברקע ומאזין לשינויים ב-Firestore.
 * השירות אחראי להקפיץ התראה למכשיר ברגע שמשתמש אחר מעלה פוסט חדש באפליקציה.
 */
public class PostsNotificationService extends Service {
    private static final String POST_CHANNEL_ID = "POST_CHANNEL_ID"; // מזהה ייחודי לערוץ ההתראות (חובה מאנדרואיד 8.0 ומעלה)
    private boolean mAfterFirstDBLoad; // מניעת התראות על פוסטים מהעבר
    private static final String TAG = "StepUp_Service"; // תגית לניהול מעקב ומציאת השירות ב-Logcat

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Service created");
    }

    /**
     * נקודת הכניסה המרכזית של השירות. מופעלת כאשר ה-Activity קוראת ל-startService.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: Service started with ID: " + startId);
        mAfterFirstDBLoad = false; // אתחול בכל הפעלה מחדש של השירות
        createPostNotificationChannel(); // יצירת ערוץ ההתראות במערכת ההפעלה

        // הפיכת השירות ל-Foreground Service על ידי שליחת התראה קבועה.
        // זה מונע ממערכת ההפעלה להרוג את השירות כשהאפליקציה נסגרת.
        Log.d(TAG, "onStartCommand: Initializing foreground notification");
        sendNotification("StepUp is active", "Looking for new posts...", 1, true);

        listenToChangesInPosts(); // הפעלת ההאזנה ל-Firestore בזמן אמת
        return START_STICKY;
    }

    /**
     * פונקציה חובה כשיורשים מ-Service. משמשת רק אם רוצים לבצע Bind (קישור) ישיר ל-Activity.
     * מכיוון שהשירות שלנו עצמאי לחלוטין ברקע, אנו מחזירים null.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Service bound");
        return null;
    }

    /**
     * פונקציה המגדירה מאזין קבוע (Snapshot Listener) על האוסף "posts" ב-Firestore.
     * כל שינוי, מחיקה או הוספה של פוסט יקפיצו את הפונקציה הזו מיד.
     */
    private void listenToChangesInPosts() {
        Log.d(TAG, "listenToChangesInPosts: Setting up Firestore listener on 'posts' collection");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        firestore.collection("posts")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        // טיפול בשגיאות תקשורת או הרשאות מול Firestore
                        if (e != null) {
                            Log.e(TAG, "onEvent: Firestore listener error!", e);
                            return;
                        }

                        if (snapshots == null) {
                            Log.w(TAG, "onEvent: Received null snapshots");
                            return;
                        }

                        Log.d(TAG, "onEvent: Received snapshot update. Document count: " + snapshots.size());

                        // לוגיקה קריטית: הטעינה הראשונה של Firestore מביאה את *כל* הפוסטים הקיימים בהיסטוריה.
                        // אנו בודקים את הדגל: אם זו הטעינה הראשונית, אנו משנים אותו ל-true ומפסיקים את הפונקציה,
                        // כדי שהמשתמש לא יקבל פתאום 50 התראות על פוסטים ישנים מהעבר.
                        if (!mAfterFirstDBLoad) {
                            Log.i(TAG, "onEvent: Initial data loaded. Skipping notifications for existing documents.");
                            mAfterFirstDBLoad = true;
                            return;
                        }

                        // ריצה רק על השינויים שקרו בפועל (Delta Changes) מאז העדכון האחרון
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            Log.d(TAG, "onEvent: Change detected: Type = " + dc.getType());

                            // סינון: אנו מעוניינים להקפיץ התראה אך ורק אם סוג השינוי הוא הוספה של מסמך חדש (ADDED)
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                String docId = dc.getDocument().getId();
                                Log.i(TAG, "onEvent: New post detected! ID: " + docId);

                                // שליחת הנתונים ליצירת ההתראה.
                                // משתמשים ב-hashCode של ה-ID של המסמך כדי לייצר מספר ייחודי (ID) עבור ההתראה.
                                sendPostNotification(dc.getDocument().getData(), docId.hashCode());
                            }
                        }
                    }
                });
    }

    /**
     * פירוק הנתונים הגולמיים שחזרו מהמסמך ב-Firestore (שם המשתמש ותיאור הפוסט)
     * והכנתם לטקסט שיוצג בתוך ההתראה.
     */
    private void sendPostNotification(Map<String, Object> post, int notificationId) {
        String userName = post.get("userName") != null ? post.get("userName").toString() : "Someone";
        String description = post.get("description") != null ? post.get("description").toString() : "posted something new!";

        Log.d(TAG, "sendPostNotification: Preparing notification for user: " + userName);
        sendNotification("New Post from " + userName, description, notificationId, false);
    }

    /**
     * הפונקציה המרכזית שבונה את אובייקט ההתראה הפיזי (Notification) ומציגה אותו למשתמש במכשיר.
     */
    private void sendNotification(String title, String content, int notificationId, boolean startForeground) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (notificationManager == null) {
            Log.e(TAG, "sendNotification: NotificationManager is null!");
            return;
        }

        // הגנה קריטית: בדיקה האם המשתמש חסם את קבלת ההתראות מהאפליקציה בהגדרות הטלפון
        if (!notificationManager.areNotificationsEnabled()) {
            Log.w(TAG, "sendNotification: Notifications are disabled by the user");
            return;
        }

        Log.d(TAG, "sendNotification: Building notification: " + title);

        // יצירת Intent שיקבע לאן המשתמש יעבור כשהוא ילחץ על ההתראה (במקרה שלנו: למסך הבית - HomeActivity)
        Intent resultIntent = new Intent(getApplicationContext(), HomeActivity.class);

        // שימוש ב-TaskStackBuilder כדי לבנות את "היסטוריית המסכים" (Back Stack).
        // זה מבטיח שאם המשתמש ילחץ על כפתור 'חזור' מתוך מסך הבית אליו הגיע מההתראה, האפליקציה תיסגר בצורה מסודרת ולא תתנהג מוזר.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        //PendingIntent (אישור מראש למערכת ההפעלה לבצע את המעבר גם כשהאפליקציה סגורה)
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // בניית העיצוב והתכונות של ההתראה
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, POST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // האייקון הקטן שיופיע בשורת הסטטוס למעלה
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // הגדרת חשיבות ההתראה
                .setContentIntent(resultPendingIntent) // קישור הפעולה שתתבצע בלחיצה
                .setAutoCancel(true); // מחיקת ההתראה משורת ההתראות ברגע שהמשתמש לחץ עליה

        Notification notification = builder.build();

        if (startForeground) {
            // החלק שהופך את השירות ל-Foreground (דורש הצגת התראה קבועה למשתמש)
            Log.i(TAG, "sendNotification: Starting service in foreground mode");
            startForeground(notificationId, notification);
        } else {
            // הקפצת התראה רגילה וחולפת למכשיר (עבור פוסט חדש)
            Log.d(TAG, "sendNotification: Posting regular notification. ID: " + notificationId);
            notificationManager.notify(notificationId, notification);
        }
    }

    /**
     * יצירת ערוץ התראות (Notification Channel).
     * חובה החל מאנדרואיד 8.0 (API 26), אחרת ההתראות פשוט לא יופיעו במכשיר.
     * מאפשר למשתמש לשלוט בנפרד על סוגי התראות (למשל להשתיק התראות פוסטים אך להשאיר התראות תזכורת).
     */
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