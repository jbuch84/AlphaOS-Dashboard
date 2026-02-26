package com.github.ma1co.pmcademo.app;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {
    public static final int PORT = 8080;

    public HttpServer() {
        super(PORT);
    }

    @Override
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        return newFixedLengthResponse(Response.Status.OK, MIME_HTML, "Hello World!");
    }
}
