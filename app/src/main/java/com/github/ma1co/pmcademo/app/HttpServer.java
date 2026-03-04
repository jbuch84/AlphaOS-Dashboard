package com.github.ma1co.pmcademo.app;

import android.content.Context;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.StatFs;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class HttpServer extends NanoHTTPD {
    public static final int PORT = 8080;
    private Context context;

    public HttpServer(Context context) {
        super(PORT);
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        File root = Environment.getExternalStorageDirectory();

        try {
            if (uri.equals("/")) {
                InputStream is = context.getAssets().open("index.html");
                return newChunkedResponse(Status.OK, "text/html", is);
            }

            if (uri.equals("/api/system")) {
                StatFs stat = new StatFs(root.getPath());
                long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
                double gbAvailable = bytesAvailable / (1024.0 * 1024.0 * 1024.0);
                boolean hasGraded = new File(root, "GRADED").exists();
                // We send back whether the folder exists so the UI can hide the button
                String json = String.format("{\"storage_gb\": \"%.1f\", \"has_graded\": %b}", gbAvailable, hasGraded);
                return newFixedLengthResponse(Status.OK, "application/json", json);
            }

            if (uri.startsWith("/api/files")) {
                Map<String, String> params = session.getParms();
                String folderParam = params.get("folder"); // "DCIM" or "GRADED"
                
                File targetDir = (folderParam != null && folderParam.equals("GRADED")) 
                                 ? new File(root, "GRADED") 
                                 : new File(root, "DCIM/100MSDCF");

                List<File> allFiles = getMediaFiles(targetDir);
                
                StringBuilder json = new StringBuilder();
                json.append("{\"folder\": \"").append(folderParam).append("\", \"files\": [");
                for (int i = 0; i < allFiles.size(); i++) {
                    File f = allFiles.get(i);
                    json.append("{\"name\":\"").append(f.getName())
                        .append("\", \"date\":").append(f.lastModified())
                        .append(", \"size\":").append(f.length()).append("}");
                    if (i < allFiles.size() - 1) json.append(",");
                }
                json.append("]}");
                return newFixedLengthResponse(Status.OK, "application/json", json.toString());
            }

            // Path for DCIM thumbnails (Fast)
            if (uri.startsWith("/thumb/")) {
                String fileName = uri.substring(7);
                File file = new File(root, "DCIM/100MSDCF/" + fileName);
                if (file.exists()) {
                    ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                    byte[] thumb = exif.getThumbnail();
                    if (thumb != null) return newFixedLengthResponse(Status.OK, "image/jpeg", new ByteArrayInputStream(thumb), thumb.length);
                }
                return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "No thumb");
            }

            if (uri.startsWith("/full/")) {
                String fileName = uri.substring(6);
                // Try Graded folder first, then DCIM
                File file = new File(root, "GRADED/" + fileName);
                if (!file.exists()) file = new File(root, "DCIM/100MSDCF/" + fileName);

                if (file.exists()) {
                    return newFixedLengthResponse(Status.OK, "image/jpeg", new FileInputStream(file), file.length());
                }
            }

        } catch (Exception e) {
            return newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
        }
        return newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "404");
    }

    private List<File> getMediaFiles(File dir) {
        List<File> result = new ArrayList<File>();
        if (!dir.exists()) return result;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory() && f.getName().toLowerCase().endsWith(".jpg")) {
                    result.add(f);
                }
            }
        }
        Collections.sort(result, new Comparator<File>() {
            public int compare(File f1, File f2) { return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified()); }
        });
        return result;
    }
}