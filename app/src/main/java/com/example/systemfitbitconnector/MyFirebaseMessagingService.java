package com.example.systemfitbitconnector;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        Log.d("FCM", "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d("FCM", "Message data payload: " + remoteMessage.getData());
        }
        if (remoteMessage.getNotification() != null) {
            Log.d("FCM", "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        // Build a notification based on the FCM message
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MyNotifications")
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a system icon
                .setContentTitle(remoteMessage.getNotification().getTitle()) // Use title from FCM message
                .setContentText(remoteMessage.getNotification().getBody()) // Use body from FCM message
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // NotificationId is a unique int for each notification that you must define
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling ActivityCompat#requestPermissions
            // here to request the missing permissions, however, it is hard to do in this class,
            // please request permission in the main activity.
            Log.d("FCM", "POST_NOTIFICATIONS permission not granted");
            return;
        }
        notificationManager.notify(new Random().nextInt(), builder.build());
    }

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "Refreshed token: " + token);
        // Send token to your server to send messages to this device.
    }
}
