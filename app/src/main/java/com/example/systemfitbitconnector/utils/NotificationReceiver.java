package com.example.systemfitbitconnector.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.Random;

/**
 * Class NotificationReceiver class is used in MyFirebaseMessagingService to trigger
 * notification once received message from Firebase.
 */
public class NotificationReceiver {
    private final Context context;
    private static final String CHANNEL_ID = "firebase_notification_channel"; // declared in MainActivity

    public NotificationReceiver(Context context) {
        this.context = context;
    }

    public void triggerNotification(String title, String body) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("Notification Permission", "POST_NOTIFICATIONS permission not granted");
            // TODO: request permission again
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX); // highest priority

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(new Random().nextInt(), builder.build());
    }
}

