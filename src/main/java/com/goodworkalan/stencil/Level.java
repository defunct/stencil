package com.goodworkalan.stencil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.goodworkalan.ilk.Ilk;

/**
 * A stack element in a stack that mirrors the decent into the document 
 * tree when an in order document is processed by Stencil.
 *
 * @author Alan Gutierrez
 */
class Level {
    /** The namespace URIs declared by this element. */
    public Map<String, String> namespaceURIs = new HashMap<String, String>();
    
    /** The prefixes declared by this element. */
    public Map<String, String> prefixes = new HashMap<String, String>();

    /**
     * Whether a start element node has been encountered. Prior to encountering
     * a start element node, the node list may contain namespace mapping nodes.
     * When a namespace mapping node is encountered and the current level
     * already has an element, it means that we need to push a new level onto
     * the stack to record the namespace delcarations that come just before a
     * start element event.
     */
    public boolean hasElement;

    /** Whether to skip forwarding events to the transformer handler. */
    public boolean skip;
    
    /** Whether the parent is a choose element. */ 
    public boolean choose;
    
    /** The type of context in a box. */
    public Ilk.Box context;

    /** The currently selected object in a box. */
    public Ilk.Box selected;
    
    /** Whether this level was created for a stencil invocation. */
    public boolean isStencil;
    
    public int indent;
    
    public boolean pre;
    
    /** The map of stencils imported by this element. */
    public Map<QName, Stencil> stencils = new HashMap<QName, Stencil>();
    
    /** The list of imports. */
    public List<Ilk.Key> imports = new ArrayList<Ilk.Key>();
}
