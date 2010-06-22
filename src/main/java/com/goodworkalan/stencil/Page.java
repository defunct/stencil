package com.goodworkalan.stencil;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Cached results of a stencil document analysis, including in order document
 * nodes and the stencils discovered in the documents.
 * 
 * @author Alan Gutierrez
 */
class Page {
    /** The URI of the XML document. */
    public final URI uri;
    
    /** The list of in order document events. */
    public final List<Object> nodes;

    /** The map of qualified stencil names to node list indicies. */
    public final Map<QName, Stencil> stencils = new HashMap<QName, Stencil>();

    /**
     * Create a page with the given list of in order document nodes.
     * 
     * @param nodes
     *            The list of nodes.
     */
    public Page(URI uri, List<Object> nodes) {
        this.uri = uri;
        this.nodes = nodes;
    }
}