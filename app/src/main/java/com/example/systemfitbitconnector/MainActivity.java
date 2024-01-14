package com.example.systemfitbitconnector;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

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

        DataThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final StringBuilder stringBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = input.readLine()) != null) {
                    stringBuilder.append(inputLine); // Read the incoming data

                }
                input.close();
                socket.close();

                // Convert StringBuilder to String
                final String receivedData = stringBuilder.toString();

                // Update the UI with the received data
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after received data (i.e. update UI / set Text View)
                        Log.d("ServerThread", "Received Data: " + receivedData);
                        // textView.setText(receivedData);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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