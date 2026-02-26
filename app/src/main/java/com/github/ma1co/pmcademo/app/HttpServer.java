package com.github.ma1co.pmcademo.app;

import android.content.Context;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.StatFs;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HttpServer extends NanoHTTPD {
    private Context context;
    private File dcimDir;

    public HttpServer(Context context) {
        super(8080);
        this.context = context;
        this.dcimDir = new File(Environment.getExternalStorageDirectory(), "DCIM");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        try {
            // 1. Serve the UI Dashboard
            if (uri.equals("/")) {
                InputStream is = context.getAssets().open("index.html");
                return Response.newChunkedResponse(Status.OK, "text/html", is);
            }

            // 2. Hardware Telemetry API (Calculates SD Card Free Space)
            if (uri.equals("/api/system")) {
                StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
                // Using legacy block methods to maintain compatibility with the a5100's older Android OS
                long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
                double gbAvailable = bytesAvailable / (1024.0 * 1024.0 * 1024.0);
                String json = String.format("{\"storage_gb\": \"%.1f\"}", gbAvailable);
                return Response.newFixedLengthResponse(Status.OK, "application/json", json);
            }

            // 3. Paginated File API (Prevents the camera from crashing on massive folders)
            if (uri.startsWith("/api/files")) {
                Map<String, List<String>> params = session.getParameters();
                int offset = params.containsKey("offset") ? Integer.parseInt(params.get("offset").get(0)) : 0;
                int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit").get(0)) : 50;

                List<File> allFiles = getMediaFiles(dcimDir);
                int total = allFiles.size();
                
                int end = Math.min(offset + limit, total);
                List<File> pageFiles = offset < total ? allFiles.subList(offset, end) : new ArrayList<File>();

                StringBuilder json = new StringBuilder();
                json.append("{\"total\": ").append(total).append(", \"files\": [");
                for (int i = 0; i < pageFiles.size(); i++) {
                    File f = pageFiles.get(i);
                    json.append("{\"name\":\"").append(f.getName())
                        .append("\", \"date\":").append(f.lastModified())
                        .append(", \"size\":").append(f.length()).append("}");
                    if (i < pageFiles.size() - 1) json.append(",");
                }
                json.append("]}");
                return Response.newFixedLengthResponse(Status.OK, "application/json", json.toString());
            }

            // 4. Instant EXIF Thumbnails (Strips out the 15KB preview instead of sending a 10MB file)
            if (uri.startsWith("/thumb/")) {
                String fileName = uri.substring(7);
                File file = findFile(dcimDir, fileName);
                if (file != null && file.exists()) {
                    try {
                        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                        byte[] imageData = exif.getThumbnail();
                        if (imageData != null) {
                            return Response.newFixedLengthResponse(Status.OK, "image/jpeg", new ByteArrayInputStream(imageData), imageData.length);
                        }
                    } catch (Exception e) {
                        // If it's a video or missing EXIF, drop down to the 404 so the HTML handles the icon
                    }
                }
                return Response.newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "No thumbnail found");
            }

            // 5. Full Resolution Image/Video Delivery
            if (uri.startsWith("/full/")) {
                String fileName = uri.substring(6);
                File file = findFile(dcimDir, fileName);
                if (file != null && file.exists()) {
                    FileInputStream fis = new FileInputStream(file);
                    String mime = fileName.toLowerCase().endsWith(".mp4") ? "video/mp4" : "image/jpeg";
                    return Response.newFixedLengthResponse(Status.OK, mime, fis, file.length());
                }
            }

        } catch (Exception e) {
            return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Server Error: " + e.getMessage());
        }

        return Response.newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found");
    }

    // Deep scans the DCIM folder to find all JPGs and MP4s, skipping Sony database files
    private List<File> getMediaFiles(File dir) {
        List<File> result = new ArrayList<File>();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    result.addAll(getMediaFiles(f));
                } else {
                    String name = f.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".mp4")) {
                        result.add(f);
                    }
                }
            }
        }
        // Sort newest photos to the top
        Collections.sort(result, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
            }
        });
        return result;
    }

    // Locates a requested file by name, searching subdirectories
    private File findFile(File dir, String fileName) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    File found = findFile(f, fileName);
                    if (found != null) return found;
                } else if (f.getName().equals(fileName)) {
                    return f;
                }
            }
        }
        return null;
    }
}