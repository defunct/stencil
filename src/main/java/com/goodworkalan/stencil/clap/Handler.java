package com.goodworkalan.stencil.clap;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * An implementation of the ClassLoader Access Protocol using the default Java
 * facilities, which are abysmal. See <a
 * href="http://java.sun.com/developer/onlineTraining/protocolhandlers/">A New
 * Era for Protocol Handlers</a> for the details, like <strong>the class name
 * and package is part of the interface</string>.
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
