package com.goodworkalan.stencil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
class Level<T> {
    /** Whether to skip forwarding events to the transformer handler. */
    public boolean skip;
    
    /** The type of context in a box. */
    public Ilk.Box ilk;

    /** The currently selected object in a box. */
    public Ilk.Box instance;
    
    /** Whether this level was created for a stencil invocation. */
    public boolean isStencil;
    
    public int indent;
    
    public boolean met;
    
    public String command;
    
    public boolean pre;
    
    public int eachIndex;
    
    public String eachAfter;

    Iterator<T> each = null;
    
    Actualizer<T> actualizer = null;
    
    /** The map of stencils imported by this element. */
    public Map<QName, Stencil> stencils = new HashMap<QName, Stencil>();
    
    /** The list of imports. */
    public List<Ilk.Key> imports = new ArrayList<Ilk.Key>();
}
