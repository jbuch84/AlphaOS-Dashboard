package com.github.ma1co.pmcademo.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends BaseActivity {
    private TextView statusText;
    private HttpServer server;
    private Handler handler = new Handler();
    private static final int TIMEOUT_MS = 10000; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 1. Build the UI programmatically (No XML required)
        statusText = new TextView(this);
        statusText.setTextSize(24);
        statusText.setTextColor(Color.WHITE);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(20, 20, 20, 20);
        
        // Set the screen to show our new text view
        setContentView(statusText);

        statusText.setText("Searching for known Wi-Fi...");
        
        // 2. Start the Server
        server = new HttpServer(this); 
        try {
            server.start();
        } catch (Exception e) {
            statusText.setText("Server Error: " + e.getMessage());
        }

        // 3. Start Connection Logic
        attemptConnection();
    }

    private void attemptConnection() {
        if (isNetworkAvailable()) {
            displayConnectionInfo("Station Mode (Home Wi-Fi)");
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isNetworkAvailable()) {
                        startWifiDirect();
                    } else {
                        displayConnectionInfo("Station Mode (Home Wi-Fi)");
                    }
                }
            }, TIMEOUT_MS);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    private void startWifiDirect() {
        statusText.setText("No network found.\nStarting Camera Hotspot...");
        // Launch the internal Hotspot
        android.content.Intent intent = new android.content.Intent(this, WifiDirectActivity.class);
        startActivity(intent);
        finish(); // Close MainActivity so it doesn't run in the background
    }

    private void displayConnectionInfo(String mode) {
        String ip = getIPAddress();
        statusText.setText("ALPHA OS ONLINE\n\n" +
                "Mode: " + mode + "\n" +
                "URL: http://" + ip + ":8080\n\n" +
                "Type this exact URL into your phone's browser.");
    }

    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr.indexOf(':') < 0) return sAddr;
                    }
                }
            }
        } catch (Exception ex) { }
        return "192.168.122.1"; // Default fallback for Direct Mode
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }
}