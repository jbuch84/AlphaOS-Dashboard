package com.github.ma1co.pmcademo.app;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
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
        
        // Ensure activity_main.xml exists in your res/layout folder
        setContentView(R.layout.activity_main);
        statusText = (TextView) findViewById(R.id.statusText);

        statusText.setText("Searching for known Wi-Fi...");
        
        // Corrected constructor for your specific HttpServer.java
        server = new HttpServer(this); 
        try {
            server.start();
        } catch (Exception e) {
            statusText.setText("Server Error: " + e.getMessage());
        }

        attemptConnection();
    }

    private void attemptConnection() {
        if (isNetworkAvailable()) {
            displayConnectionInfo("Station Mode");
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isNetworkAvailable()) {
                        startWifiDirect();
                    } else {
                        displayConnectionInfo("Station Mode");
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
        // This must match the actual start method in your WifiDirectActivity
        WifiDirectActivity.start(this); 
    }

    private void displayConnectionInfo(String mode) {
        String ip = getIPAddress();
        statusText.setText("ALPHA OS ONLINE\n" +
                "Mode: " + mode + "\n" +
                "URL: http://" + ip + ":8080\n\n" +
                "Open this link on your phone.");
    }

    // Integrated IP lookup to fix the "Missing IPUtils" error
    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':')<0;
                        if (isIPv4) return sAddr;
                    }
                }
            }
        } catch (Exception ex) { }
        return "192.168.122.1"; // Fallback for Direct Mode
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }
}