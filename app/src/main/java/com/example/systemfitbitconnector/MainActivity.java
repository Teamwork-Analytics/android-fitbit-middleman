package com.example.systemfitbitconnector;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "FitbitConnector";

    private static final int HOSTING_PORT = 3000;
    private static final int CONNECT_TIMEOUT_MS = 4000;
    private static final int READ_TIMEOUT_MS = 6000;

    // phone preferences store
    private static final String PREFS = "cfg";
    private static final String PREF_PC_IP = "pcServerIp";
    private static final String PREF_ROLE = "actualUser";

    private ServerSocket serverSocket;
    Thread serverThread = null;

    // Rendezvous & state
    private static final String RENDEZVOUS_RESOLVE_URL = "https://colam.jiexiangfan.com/api/resolve";
    private String pcServerIp = "49.127.33.177"; // Hardcoded ip for stability/fallback
    private volatile String actualUser = "blue"; // user role/colour. E.g. `blue`

    // UI refs
    private TextView infoText;
    private Button refreshButton;
    private EditText roleInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---- UI wiring ----
        infoText = findViewById(R.id.infoText);
        refreshButton = findViewById(R.id.refreshButton);
        roleInput = findViewById(R.id.roleInput);

        // Load cached settings
        String cachedIp = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_PC_IP, null);
        if (cachedIp != null && !cachedIp.isEmpty()) pcServerIp = cachedIp;
        String cachedRole = getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREF_ROLE, null);
        if (cachedRole != null && !cachedRole.isEmpty()) actualUser = cachedRole;

        // Reflect initial state in UI
        roleInput.setText(actualUser);
        roleInput.addTextChangedListener(new SimpleTextWatcher(s -> {
            actualUser = s.trim();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_ROLE, actualUser).apply();
            updateInfoUi();
        }));
        updateInfoUi();

        // Resolve latest IP (non-blocking) — disable button while resolving
        setRefreshing(true);
        resolvePcFromRendezvousAsync();

        // Start ServerThread for receiving and forwarding data
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close server socket first to unblock accept()
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            Log.w(TAG, "Server socket close failed", e);
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        // (no global executor to shutdown; we shutdown per-task below)
    }

    // ===== UI actions =====
    public void onRefreshBtnClick(View v) {
        setRefreshing(true);
        resolvePcFromRendezvousAsync();
    }


    private void setRefreshing(boolean refreshing) {
        if (refreshButton == null) return;
        runOnUiThread(() -> {
            refreshButton.setEnabled(!refreshing);
            refreshButton.setText(refreshing ? "Getting destination server IP..." : "Refresh IP");
        });
    }

    private void updateInfoUi() {
        if (infoText == null) return;
        runOnUiThread(() -> infoText.setText(
                "Device role (colour): " + actualUser + "\n" +
                        "Destination Server IP: " + pcServerIp + "\n"
        ));
    }


    // ===== Resolve PC IP from rendezvous API =====
    private void resolvePcFromRendezvousAsync() {
        new Thread(() -> {
            HttpURLConnection c = null;
            try {
                URL u = new URL(RENDEZVOUS_RESOLVE_URL);
                c = (HttpURLConnection) u.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(CONNECT_TIMEOUT_MS);
                c.setReadTimeout(READ_TIMEOUT_MS);

                int code = c.getResponseCode();
                if (code == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (InputStream is = c.getInputStream();
                         BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject j = new JSONObject(sb.toString());
                    String ip = j.optString("ipAddress", null); // API shape: { deviceId, ipAddress }
                    if (ip != null && !ip.isEmpty()) {
                        pcServerIp = ip;
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_PC_IP, pcServerIp).apply();
                        Log.i(TAG, "Resolved PC IP: " + pcServerIp);
                    } else {
                        Log.w(TAG, "Resolve returned empty ipAddress; keep " + pcServerIp);
                    }
                } else {
                    Log.w(TAG, "Resolve HTTP " + code + "; keep " + pcServerIp);
                }
            } catch (Exception e) {
                Log.w(TAG, "Resolve failed; keep " + pcServerIp, e);
            } finally {
                if (c != null) c.disconnect();
                setRefreshing(false);
                updateInfoUi();
            }
        }, "ResolveThread").start();
    }

    // ===== Local server that accepts and forwards =====
    private class ServerThread implements Runnable {
        public void run() {
            try {
                serverSocket = new ServerSocket(HOSTING_PORT);
                serverSocket.setReuseAddress(true);
                Log.i(TAG, "Listening on :" + HOSTING_PORT);
            } catch (Exception e) {
                Log.e(TAG, "Open server socket failed", e);
                return;
            }
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket socket = serverSocket.accept(); // blocking
                    new DataThread(socket).start();
                } catch (Exception e) {
                    if (serverSocket == null || serverSocket.isClosed()) break;
                    Log.e(TAG, "accept() failed", e);
                }
            }
        }
    }

    // ===== Handles a single incoming request and forwards it =====
    class DataThread extends Thread {
        private final Socket socket;
        private final ExecutorService executor = Executors.newSingleThreadExecutor(); // per-connection; we will shut down

        DataThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (Socket s = socket) {
                s.setSoTimeout(READ_TIMEOUT_MS);

                BufferedInputStream in = new BufferedInputStream(s.getInputStream());

                // --- Read headers as BYTES until CRLFCRLF or LFLF ---
                ByteArrayOutputStream headerBuf = new ByteArrayOutputStream(512);
                int b;
                while ((b = in.read()) != -1) {
                    headerBuf.write(b);
                    byte[] hb = headerBuf.toByteArray();
                    int len = hb.length;
                    // end-of-headers: \r\n\r\n or \n\n
                    if (len >= 4 && hb[len - 4] == '\r' && hb[len - 3] == '\n' && hb[len - 2] == '\r' && hb[len - 1] == '\n') break;
                    if (len >= 2 && hb[len - 2] == '\n' && hb[len - 1] == '\n') break;
                    // keep reading otherwise
                }
                String headers = new String(headerBuf.toByteArray(), StandardCharsets.US_ASCII);

                // --- Parse Content-Length (case-insensitive) ---
                int contentLength = -1;
                for (String line : headers.split("\r\n")) {
                    if (line.regionMatches(true, 0, "Content-Length:", 0, "Content-Length:".length())) {
                        try { contentLength = Integer.parseInt(line.split(":", 2)[1].trim()); } catch (NumberFormatException ignored) {}
                    }
                }
                if (contentLength < 0) {
                    // Some senders use just \n; handle that too
                    for (String line : headers.split("\n")) {
                        if (line.trim().toLowerCase().startsWith("content-length:")) {
                            try { contentLength = Integer.parseInt(line.split(":", 2)[1].trim()); } catch (NumberFormatException ignored) {}
                        }
                    }
                }
                if (contentLength <= 0) {
                    Log.w(TAG, "No/invalid Content-Length; dropping request.\nHeaders:\n" + headers);
                    return;
                }

                // --- Read body BYTES (exactly contentLength) ---
                byte[] body = new byte[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = in.read(body, read, contentLength - read);
                    if (r == -1) break;
                    read += r;
                }
                if (read != contentLength) {
                    Log.w(TAG, "Short body read: expected " + contentLength + " bytes, got " + read);
                }
                final String receivedData = new String(body, 0, read, StandardCharsets.UTF_8);

                // Forward off-thread, then shutdown this per-connection executor to avoid leaks
                executor.execute(() -> {
                    try {
                        Log.d(TAG, "Received: " + summarize(receivedData));
                        String modifiedData = modifyUserData(receivedData);
                        forwardDataToNodeJsServer(modifiedData);
                    } finally {
                        executor.shutdown();
                    }
                });

            } catch (java.net.SocketTimeoutException ste) {
                Log.e(TAG, "DataThread timeout while reading request/body", ste);
            } catch (Exception e) {
                Log.e(TAG, "DataThread error", e);
            }
        }



        private String modifyUserData(String data) {
            try {
                JSONObject jsonObject = new JSONObject(data);
                jsonObject.put("user", actualUser); // use editable field
                return jsonObject.toString();
            } catch (Exception e) {
                Log.w(TAG, "JSON modify failed; forwarding raw", e);
                return data;
            }
        }

        private void forwardDataToNodeJsServer(String data) {
            String ip = pcServerIp;
            if (ip == null || ip.isEmpty()) {
                Log.e(TAG, "PC Server IP is not available.");
                return;
            }
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + ":3168/data");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setDoOutput(true);

                byte[] payload = data.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Forward OK (" + responseCode + ")");
                } else {
                    Log.d(TAG, "Forward non-OK (" + responseCode + ")");
                }
            } catch (Exception e) {
                Log.e(TAG, "Forward failed to " + ip, e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }

    private static String summarize(String s) {
        if (s == null) return "null";
        int max = 300;
        return s.length() <= max ? s : s.substring(0, max) + "…(" + s.length() + " chars)";
    }

    // ---- tiny TextWatcher helper ----
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        interface OnChange { void onChange(String s); }
        private final OnChange cb;
        SimpleTextWatcher(OnChange cb) { this.cb = cb; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void afterTextChanged(android.text.Editable s) { cb.onChange(s.toString()); }
    }
}