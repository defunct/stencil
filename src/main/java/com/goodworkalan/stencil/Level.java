package com.goodworkalan.stencil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.inject.Injector;

/**
 * A stack element in a stack that mirrors the decent into the document logic
 * tree when a document is processed by Stencil.
 * 
 * @author Alan Gutierrez
 */
class Level<T> {
    /** The injector. */
    public Injector injector;

    /** Whether to skip forwarding events to the transformer handler. */
    public boolean skip;
    
    /** The type of context in a box. */
    public Ilk.Box ilk;

    /** The currently selected object in a box. */
    public Ilk.Box instance;
    
    /** Whether this level was created for a stencil invocation. */
    public boolean isStencil;
    
    /** The depth of the indent before the command if it is a block command. */
    public int indent;
    
    /** Whether or not one of the conditions of an if/else latter has been met. */
    public boolean met;
    
    /** The command. */
    public String command;
    
    /** The index where this command began. */
    public int index;
    
    /** The content after this command on the line where it began. */
    public String after;
    
    /** The escapers used to escape character sequences. */
    public Map<String, Escaper> escapers = new HashMap<String, Escaper>();

    /** The iterator of the collection traversed by each. */
    public Iterator<T> each = null;
    
    /** The type collection for the collection traversed by each. */
    public Actualizer<T> actualizer = null;
    
    /** The map of stencils imported by this element. */
    public Map<String, Stencil> stencils = new HashMap<String, Stencil>();
    
    /** The list of classes brought into the Java namespace. */
    public Map<String, Ilk.Key> classes = new HashMap<String, Ilk.Key>();
}
