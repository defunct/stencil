package com.agtrz.stencil.clap;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An implementation of the ClassLoader Access Protocol  
 *
 * @author Alan Gutierrez
 */
public class Handler extends URLStreamHandler {
    @Override
    public URLConnection openConnection(URL u) throws IOException {
        String host = u.getHost();
        ClassLoader classLoader = null;
        if ("thread".equals(host)) {
            classLoader = Thread.currentThread().getContextClassLoader();
        } else if ("system".equals(host)) {
            classLoader = ClassLoader.getSystemClassLoader();
        } else if ("class".equals(host)) {
            classLoader = getClass().getClassLoader();
        }
        return classLoader.getResource(u.getPath()).openConnection();
    }
    
    @Override
    public void parseURL(URL u, String spec, int start, int limit) {
        super.parseURL(u, spec, start, limit);
        String host = u.getHost();
        if (!("thread".equals(host) || "system".equals(host) || "class".equals(host))) {
            throw new IllegalArgumentException();
        }
    }
}
