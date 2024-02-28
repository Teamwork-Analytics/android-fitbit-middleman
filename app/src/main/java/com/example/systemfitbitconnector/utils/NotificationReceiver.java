package com.example.systemfitbitconnector.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class NotificationReceiver {
    private final Context context;

    public NotificationReceiver(Context context) {
        this.context = context;
    }

    public void triggerNotification(String title, String body) {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.d("NotificationReceiver", "POST_NOTIFICATIONS permission not granted");
            // TODO: try to handle and request permission again in the app
            return;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "TeamworkAnalyticNotification")
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX); // highest priority

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(new Random().nextInt(), builder.build());
    }
}

