package com.github.ma1co.pmcademo.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.widget.TextView;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends Activity {
    private DashboardServer server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Alpha Pro Dashboard Running...\nConnect to the camera's Wi-Fi and open a browser to http://192.168.1.173:8080");
        tv.setTextSize(20);
        tv.setPadding(30, 30, 30, 30);
        setContentView(tv);

        try {
            server = new DashboardServer();
            server.start();
        } catch (Exception e) {
            tv.setText("Server failed to start: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (server != null) server.stop();
    }

    private class DashboardServer extends NanoHTTPD {
        private static final String DCIM_PATH = "/mnt/sdcard/DCIM/100MSDCF/";

        public DashboardServer() {
            super(8080);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            Map<String, String> params = session.getParms();

            try {
                // 1. Serve the HTML Dashboard
                if (uri.equals("/") || uri.equals("/index.html")) {
                    InputStream is = getAssets().open("index.html");
                    return newChunkedResponse(Response.Status.OK, "text/html", is);
                }

                // 2. Serve the Paginated API
                if (uri.equals("/api/files")) {
                    File dir = new File(DCIM_PATH);
                    File[] files = dir.listFiles();
                    if (files == null) files = new File[0];

                    Arrays.sort(files, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                        }
                    });

                    int offset = params.containsKey("offset") ? Integer.parseInt(params.get("offset")) : 0;
                    int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit")) : 50;
                    int end = Math.min(offset + limit, files.length);

                    StringBuilder json = new StringBuilder("{\"total\":" + files.length + ",\"files\":[");
                    boolean first = true;
                    
                    for (int i = offset; i < end; i++) {
                        File f = files[i];
                        if (f.getName().toUpperCase().endsWith(".JPG") || f.getName().toUpperCase().endsWith(".MTS")) {
                            if (!first) json.append(",");
                            json.append(String.format("{\"name\":\"%s\",\"date\":%d,\"size\":%d}", 
                                f.getName(), f.lastModified(), f.length()));
                            first = false;
                        }
                    }
                    json.append("]}");
                    
                    Response res = newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
                    res.addHeader("Access-Control-Allow-Origin", "*");
                    return res;
                }

                // 3. Serve Hardware Telemetry
                if (uri.equals("/api/system")) {
                    StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
                    long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
                    long gbAvailable = bytesAvailable / (1024 * 1024 * 1024);
                    
                    String json = String.format("{\"storage_gb\":%d}", gbAvailable);
                    Response res = newFixedLengthResponse(Response.Status.OK, "application/json", json);
                    res.addHeader("Access-Control-Allow-Origin", "*");
                    return res;
                }

                // 4. Extract and Serve EXIF Thumbnails Safely
                if (uri.startsWith("/thumb/")) {
                    String filename = uri.replace("/thumb/", "");
                    if (filename.toUpperCase().endsWith(".MTS")) {
                         return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "No video thumbs");
                    }
                    
                    ExifInterface exif = new ExifInterface(DCIM_PATH + filename);
                    byte[] imageData = exif.getThumbnail();
                    
                    if (imageData != null) {
                        Response res = newFixedLengthResponse(Response.Status.OK, "image/jpeg", new ByteArrayInputStream(imageData), imageData.length);
                        res.addHeader("Cache-Control", "public, max-age=31536000"); 
                        res.addHeader("Access-Control-Allow-Origin", "*");
                        return res;
                    } else {
                        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "No EXIF thumb");
                    }
                }

                // 5. Serve Full High-Res Files
                if (uri.startsWith("/full/")) {
                    String filename = uri.replace("/full/", "");
                    File f = new File(DCIM_PATH + filename);
                    Response res = newChunkedResponse(Response.Status.OK, "image/jpeg", new FileInputStream(f));
                    res.addHeader("Access-Control-Allow-Origin", "*");
                    return res;
                }

            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server Error");
            }
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Endpoint not found");
        }
    }
}