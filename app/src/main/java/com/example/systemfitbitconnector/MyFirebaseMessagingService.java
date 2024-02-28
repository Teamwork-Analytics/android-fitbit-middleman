package com.example.systemfitbitconnector;

import com.example.systemfitbitconnector.utils.NotificationReceiver;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.util.Log;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Handle FCM messages here
        Log.d("FCM", "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d("FCM", "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            // logging fcm data received
            Log.d("FCM", "Message Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d("FCM", "Message Notification Body: " + remoteMessage.getNotification().getBody());

            NotificationReceiver notificationHelper = new NotificationReceiver(this);
            if (remoteMessage.getNotification() != null) {
                notificationHelper.triggerNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
            }
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "Refreshed token: " + token);
        // TODO: Send token to your server to send messages to this device.
    }
}
