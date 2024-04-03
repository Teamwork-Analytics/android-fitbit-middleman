package com.example.systemfitbitconnector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    // TODO: try configure app to increase background runtime/ always run on background

    private static final int HOSTING_PORT = 3000;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private static final String FIREBASE_NOTIFICATION_CHANNEL_ID = "firebase_notification_channel";
    private static final String FIREBASE_NOTIFICATION_CHANNEL_NAME = "Analytic Simulation Messaging Service";
    private static final String FIREBASE_NOTIFICATION_CHANNEL_DESCRIPTION = "Notification channel for messaging teaching team/nurses via Firebase messaging service.";

    private ServerSocket serverSocket;
    Thread serverThread = null;

    // Life-Cycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // start ServerThread for receiving and forwarding data
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestNotificationPermission();
        createNotificationChannel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // shutdown DataThread's executor
        if (serverThread != null) {
            serverThread.interrupt();
        }
        // close server socket when activity is destroyed
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Override Methods
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Do action that requires this permission or display messages.
            } else {
                // Permission denied. Handle the feature without this permission or inform the user as necessary.
            }
        }
    }

    /**
     * Method to request notification permission. Triggered when the app is installed or restarted
     */
    private void requestNotificationPermission(){
        // not checking sdk version as the code is built for the latest API (34)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Method to create notification channel. The chanel created will be used in NotificationReceiver.
     */
    private void createNotificationChannel(){
        int notificationImportance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(FIREBASE_NOTIFICATION_CHANNEL_ID,
                FIREBASE_NOTIFICATION_CHANNEL_NAME, notificationImportance);
        channel.setDescription(FIREBASE_NOTIFICATION_CHANNEL_DESCRIPTION);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    /**
     * ServerThread receive data on HOSTING_PORT then forward using DataThread.
     */
    private class ServerThread implements Runnable {
        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(HOSTING_PORT); // Create the ServerSocket
            } catch (Exception e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept(); // Accept incoming connections
                    // Handle the accepted connection
                    DataThread dataThread = new DataThread(socket);
                    dataThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * DataThread send the data to pc server.
     */
    class DataThread extends Thread {
        private Socket socket;
        private ExecutorService executor = Executors.newSingleThreadExecutor(); // Shared executor

        DataThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String inputLine;
                int contentLength = -1;

                // Read headers
                while ((inputLine = input.readLine()) != null && !inputLine.isEmpty()) {
                    if (inputLine.startsWith("Content-Length:")) {
                        contentLength = Integer.parseInt(inputLine.split(": ")[1].trim());
                    }
                    // Avoid logging the header
                    // Log.d("ServerThread", "Received Header: " + inputLine);
                }

                // Read body if Content-Length is found
                if (contentLength > -1) {
                    char[] buffer = new char[contentLength];
                    input.read(buffer, 0, contentLength);
                    final String receivedData = new String(buffer);

                    executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("ServerThread", "Received Data: " + receivedData);
                            forwardDataToNodeJsServer(receivedData);
                        }
                    });
                }

                input.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            executor.shutdownNow(); // Shutdown the executor
        }

        private void forwardDataToNodeJsServer(String data) {
            try {
                URL url = new URL("http://49.127.54.107:3168/data"); // Replace with actual server URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes("UTF-8")); // Specify charset
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Handle success
                    Log.d("DataThread", "Successfully forwarded data. Response: " + responseCode);
                } else {
                    // Handle server error
                    Log.d("DataThread", "Error forwarding data. Response: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}