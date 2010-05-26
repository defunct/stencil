package com.goodworkalan.stencil;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * A factory for creating URLs from URIs that does not require altering
 * the system properties to support custom protocols.
 *
 * @author Alan Gutierrez
 */
public class URLFactory {
    /** A map of protocols to stream handlers. */
    private final Map<String, URLStreamHandler> streamHandlers = new HashMap<String, URLStreamHandler>();

    /**
     * Get a URL for the given URI.
     * 
     * @param uri
     *            The URI.
     * @return A URL using one of our mapped stream handlers or the default
     *         stream stream handlers if the protocol is not mapped.
     * @throws MalformedURLException
     */
    public URL getInstance(URI uri) throws MalformedURLException {
        URLStreamHandler streamHandler = streamHandlers.get(uri.getScheme());
        if (streamHandler == null) {
            return uri.toURL();
        }
        return new URL(null, uri.toString(), streamHandler);
    }

    /**
     * Map the given protocol to the given URL stream handler.
     * 
     * @param protocol
     *            The protocol.
     * @param streamHandler
     *            The URL stream handler.
     */
    public void map(String protocol, URLStreamHandler streamHandler) {
        streamHandlers.put(protocol, streamHandler);
    }
}
