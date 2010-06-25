package com.goodworkalan.stencil;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cached results of a stencil document analysis, including in order document
 * nodes and the stencils discovered in the documents.
 * 
 * @author Alan Gutierrez
 */
class Page {
    /** The URI of the XML document. */
    public final URI uri;

    /** The last modified time of the resource. */
    public final long lastModified;

    /** The list of lines in the document. */
    public final List<String> lines;

    /** The map of qualified stencil names to line indexes. */
    public final Map<String, Stencil> stencils = new HashMap<String, Stencil>();

    /**
     * Create a page with the given list of in order document nodes.
     * 
     * @param uri
     *            The URI of the XML document.
     * @param lastModified
     *            The last modified time of the resource.
     * @param lines
     *            The list of lines in the document.
     */
    public Page(URI uri, long lastModified, List<String> lines) {
        this.uri = uri;
        this.lines = lines;
        this.lastModified = lastModified;
    }
}