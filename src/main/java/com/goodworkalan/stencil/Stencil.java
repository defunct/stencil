package com.goodworkalan.stencil;

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

    /** The offset of the second line in the list of nodes. */
    public final int index;

    /** The remaining part of the first line. */
    public final String after;
    
    public final int count;

    /** The namespace URIs declared when the stencil was declared. */
    public Map<String, String> namespaceURIs = new HashMap<String, String>();

    /** The prefixes declared when the stencil was declared. */
    public Map<String, String> prefixes = new HashMap<String, String>();

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
        this.count = 0;
        this.index = 0;
        this.after = null;
    }

    /**
     * Create a stencil that processes the remaining part of a line given by
     * after, and resuming at the given index.
     * 
     * @param page
     *            The page.
     * @param after
     *            The remainder of the current line to process.
     * @param index
     *            The next index to process.
     */
    public Stencil(Page page, String after, int index, int count) {
        this.page = page;
        this.after = after;
        this.index = index;
        this.count = count;
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
        this.after = stencil.after;
        this.count = stencil.count;
    }
}
