package com.example.systemfitbitconnector;

import com.example.systemfitbitconnector.utils.NotificationReceiver;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.util.Log;

/**
 * Firebase class to enable the FCM (Firebase cloud messaging) service.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /**
     * Method to handle messages received from Firebase
     * @param remoteMessage Remote message that has been received.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Handle FCM messages here
        Log.d("FCM", "From: " + remoteMessage.getFrom());
        if (remoteMessage.getData().size() > 0) {
            Log.d("FCM", "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            NotificationReceiver notificationHelper = new NotificationReceiver(this);
            if (remoteMessage.getNotification() != null) {
                notificationHelper.triggerNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
            }

            // logging fcm data received
            Log.d("FCM", "Message Notification Title: " + remoteMessage.getNotification().getTitle());
            Log.d("FCM", "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    /**
     * Each device/application is associated to a token, this method is triggered when the token
     * of the application refreshed.
     *
     * @param token The token used for sending messages to this application instance. This token is
     *     the same as the one retrieved by {@link FirebaseMessaging#getToken()}.
     */
    @Override
    public void onNewToken(String token) {
        Log.d("FCM", "FCM token refreshed: " + token);
        // TODO: Send token to your server to send messages to this device.
    }
}
