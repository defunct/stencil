package com.goodworkalan.stencil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A stencil definition.
 * 
 * @author Alan Gutierrez
 */
class Stencil {
    /** The in order document events and uri. */
    public final Page page;
    
    /** The offset of the stencil in the list of nodes. */
    public final int index;

    /** The namespace URIs declared when the stencil was declared. */
    public Map<String, String> namespaceURIs = new HashMap<String, String>();
    
    /** The prefixes declared when the stencil was declared. */
    public Map<String, String> prefixes = new HashMap<String, String>();

    /**
     * Create an empty stencil.
     */
    public Stencil() {
        this(new Page(null, 0, Collections.<String>emptyList()), 0);
    }

    /**
     * Create a stencil using the nodes at the given index in the given list of
     * in order document events.
     * 
     * @param page
     *            The in order document events and uri.
     * @param index
     *            The offset of the stencil in the list of nodes.
     */
    public Stencil(Page page, int index) {
        this.page = page;
        this.index = index;
    }

    /**
     * Create a stencil that is a copy of the given stencil.
     * 
     * @param stencil
     *            The stencil to copy.
     */
    public Stencil(Stencil stencil) {
        this.page = stencil.page;
        this.index = stencil.index;
    }
}
