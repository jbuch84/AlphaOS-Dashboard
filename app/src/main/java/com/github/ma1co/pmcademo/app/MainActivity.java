package com.github.ma1co.pmcademo.app;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.widget.TextView;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MainActivity extends Activity {
    private HttpServer server;
    private TextView textView;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        textView = new TextView(this);
        textView.setTextSize(20);
        textView.setPadding(30, 30, 30, 30);
        setContentView(textView);

        textView.setText("Starting Camera Wi-Fi Hotspot...");

        // 1. Initialize Wi-Fi Manager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 2. Force the camera to broadcast its own Access Point
        startHotspot();

        // 3. Start the custom File Server
        server = new HttpServer(this);
        try {
            server.start();
        } catch (IOException e) {
            textView.setText("Server Error: " + e.getMessage());
            return;
        }

        // 4. Retrieve the actual Hotspot IP (not the router IP)
        String ipAddress = getHotspotIPAddress();
        if (ipAddress == null) {
            ipAddress = "192.168.43.1"; // Default Sony/Android AP IP fallback
        }

        // 5. Update the camera screen with connection instructions
        textView.setText("AlphaProManager Active\n\n" +
                         "1. Connect phone/PC to this camera's Wi-Fi network.\n" +
                         "2. Open your browser to:\n\n" +
                         "http://" + ipAddress + ":8080");
    }

    private void startHotspot() {
        try {
            // Turn off regular Wi-Fi so the chip can switch to Hotspot mode
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }
            
            // Android 4.x (Sony API) uses reflection to start the Hotspot
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, null, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopHotspot() {
        try {
            Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifiManager, null, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getHotspotIPAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                // Filter for wireless or access point interfaces
                if (intf.getName().contains("wlan") || intf.getName().contains("ap")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        // Get the IPv4 address
                        if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
        stopHotspot();
    }
}