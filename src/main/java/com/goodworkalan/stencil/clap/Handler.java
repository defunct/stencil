package com.goodworkalan.stencil.clap;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.stencil.ResourceResolver;

/**
 * An implementation of the ClassLoader Access Protocol using the default Java
 * facilities, which are abysmal. See <a
 * href="http://java.sun.com/developer/onlineTraining/protocolhandlers/">A New
 * Era for Protocol Handlers</a> for the details, like <strong>the class name
 * and package is part of the interface</string>.
 * <p>
 * I've provided an alternative to registering this little monster using the
 * global variable exposed in the Java API.
 * 
 * @author Alan Gutierrez
 */
public class Handler extends URLStreamHandler implements ResourceResolver {
    /**
     * Get the clap URL for the given uri.
     * 
     * @param injector
     *            The injector.
     * @param uri
     *            The URI.
     */
    public URL getURL(Injector injector, URI uri) throws MalformedURLException {
        return new URL(null, uri.toString(), this);
    }

    /**
     * Open a class path resource indicated by the given URL. The host part of
     * the URL is used to determine if the thread class loader or the system
     * class loader should be used. The class class loader is cannot be
     * referenced through this implementation.
     * 
     * @param u
     *            The URL.
     * @return A URL connection to read the given URL.
     * @throws IOException
     *             For any I/O error of if the resource cannnot be found.
     */
    @Override
    public URLConnection openConnection(URL u) throws IOException {
        String host = u.getHost();
        ClassLoader classLoader = chooseClassLoader(host);
        return classLoader.getResource(u.getPath().substring(1)).openConnection();
    }

    /**
     * Choose the class loader based on the host string. The host string is one
     * of "thread" for the thread class loader, "system" for the system class
     * loader, otherwise the class loader of this <code>Handler</code> instance
     * is returned by default.
     * 
     * @param host
     *            The host string.
     * @return The class loader indicated by the host string.
     */
    ClassLoader chooseClassLoader(String host) {
        if ("thread".equals(host)) {
            return Thread.currentThread().getContextClassLoader();
        }
        if ("system".equals(host)) {
            return ClassLoader.getSystemClassLoader();
        } 
        return getClass().getClassLoader();
    }

    /**
     * Assert that the given URL is correct for the CLAP URL scheme by checking
     * that the host name is either "thread" or "system".
     * 
     * @param u
     *            The URL that will be assigned the results of the parse.
     * @param spec
     *            The string to parse.
     * @param start
     *            The index of the first character to parse.
     * @param limit
     *            The length of the region to parse.
     * @exception IllegalArgumentException
     *                If the host is neither "thread" or "system".
     */
    @Override
    public void parseURL(URL u, String spec, int start, int limit) {
        super.parseURL(u, spec, start, limit);
        String host = u.getHost();
        if (!("thread".equals(host) || "system".equals(host) || "class".equals(host))) {
            throw new IllegalArgumentException("Invalid host type."); 
        }
    }
}
