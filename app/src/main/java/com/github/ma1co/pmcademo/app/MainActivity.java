package com.github.ma1co.pmcademo.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import com.github.ma1co.openmemories.framework.DeviceInfo;

public class MainActivity extends BaseActivity {
    private TextView statusText;
    private HttpServer server;
    private Handler handler = new Handler();
    private static final int TIMEOUT_MS = 10000; // 10 second timeout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = (TextView) findViewById(R.id.statusText);

        statusText.setText("Searching for known Wi-Fi...");
        
        // Start the server immediately on the default port
        server = new HttpServer(8080, getAssets());
        try {
            server.start();
        } catch (Exception e) {
            statusText.setText("Server Error: " + e.getMessage());
        }

        attemptConnection();
    }

    private void attemptConnection() {
        // Check for existing connection (Station Mode)
        if (isNetworkAvailable()) {
            displayConnectionInfo("Home/Station Mode");
        } else {
            // Fallback to Wi-Fi Direct after 10 seconds if still disconnected
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isNetworkAvailable()) {
                        startWifiDirect();
                    } else {
                        displayConnectionInfo("Home/Station Mode");
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
        // This utilizes the PMCA-specific WifiDirect trigger
        // In Direct Mode, the IP is almost always 192.168.122.1
        WifiDirectActivity.start(this); 
        finish(); 
    }

    private void displayConnectionInfo(String mode) {
        String ip = "Unknown";
        try {
            // Logic to fetch current IP from NetworkInterface
            ip = IPUtils.getIPAddress(true); 
        } catch (Exception e) {}

        statusText.setText("ALPHA OS ONLINE\n" +
                "Mode: " + mode + "\n" +
                "URL: http://" + ip + ":8080\n\n" +
                "Open this link on your phone.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }
}