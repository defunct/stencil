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

	// TODO Document.
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
        return classLoader.getResource(u.getPath().substring(1)).openConnection();
    }
    
	// TODO Document.
    @Override
    public void parseURL(URL u, String spec, int start, int limit) {
        super.parseURL(u, spec, start, limit);
        String host = u.getHost();
        if (!("thread".equals(host) || "system".equals(host) || "class".equals(host))) {
            throw new IllegalArgumentException();
        }
    }
}
