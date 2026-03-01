package com.github.ma1co.pmcademo.app;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class MainActivity extends BaseActivity {
    private TextView statusText;
    // STATIC ensures the server stays alive even if we switch screens
    private static HttpServer server; 
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Force the Sony Wi-Fi chip to turn on
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        // 2. Start server ONCE using App Context so it doesn't leak memory
        if (server == null) {
            try {
                server = new HttpServer(getApplicationContext());
                server.start();
            } catch (Exception e) {}
        }

        // 3. Build the UI Menu
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);

        statusText = new TextView(this);
        statusText.setTextSize(20);
        statusText.setTextColor(Color.WHITE);
        statusText.setGravity(Gravity.CENTER);
        statusText.setText("Alpha OS Dashboard\nChoose Connection:");
        statusText.setPadding(0, 0, 0, 40);

        Button btnHome = new Button(this);
        btnHome.setText("1. Connect to Home Wi-Fi");
        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkHomeWifi();
            }
        });

        Button btnHotspot = new Button(this);
        btnHotspot.setText("2. Start Camera Hotspot (Travel)");
        btnHotspot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWifiDirect();
            }
        });

        layout.addView(statusText);
        layout.addView(btnHome);
        layout.addView(btnHotspot);

        setContentView(layout);
    }

    private void checkHomeWifi() {
        statusText.setText("Waking up Wi-Fi chip...\nPlease wait 5 seconds.");
        
        // Give the camera time to actually connect to the router
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isNetworkAvailable()) {
                    String ip = getIPAddress();
                    statusText.setText("CONNECTED (HOME WI-FI)\n\nGo to: http://" + ip + ":8080");
                } else {
                    statusText.setText("FAILED.\nCheck your router or use the Hotspot.");
                }
            }
        }, 5000); 
    }

    private void startWifiDirect() {
        // We launch the hotspot screen, but DO NOT kill the server
        statusText.setText("Starting Hotspot...");
        Intent intent = new Intent(this, WifiDirectActivity.class);
        startActivity(intent);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
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
        return "192.168.122.1";
    }
}