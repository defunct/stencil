package com.goodworkalan.stencil;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.goodworkalan.ilk.inject.Injector;

/**
 * Resolve a URI into a URL. 
 *
 * @author Alan Gutierrez
 */
public interface ResourceResolver {
	/**
	 * Get the URL for the given URI using the given injector to obtain any
	 * instance specific objects.
	 * 
	 * @param injector
	 *            The injector.
	 * @param uri
	 *            The URI to resolve.
	 * @return A URL for the given URI.
	 * @throws MalformedURLException
	 *             If the URL is malformed.
	 */
	public URL getURL(Injector injector, URI uri) throws MalformedURLException;
}
