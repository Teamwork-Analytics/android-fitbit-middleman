package com.example.systemfitbitconnector;

import androidx.appcompat.app.AppCompatActivity;

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

// TODO: make this app run in background

public class MainActivity extends AppCompatActivity {

    private ServerSocket serverSocket;
    Thread serverThread = null;
    private static final int SERVERPORT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Starting the Server Thread
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    class ServerThread implements Runnable {
        public void run() {
            Socket socket = null;
            try {
                serverSocket = new ServerSocket(SERVERPORT); // Create the ServerSocket
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
                URL url = new URL("http://49.127.36.41:3168/data"); // Replace with actual server URL
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown DataThread's executor
        if (serverThread != null) {
            serverThread.interrupt();
        }
        try {
            // Close the server socket when the activity is destroyed
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}