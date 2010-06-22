package com.goodworkalan.stencil;

import java.util.HashMap;
import java.util.Map;

import com.goodworkalan.ilk.Ilk;

// TODO Document.
public class Level {
    // TODO Document.
    public Map<String, String> namespaceURIs = new HashMap<String, String>();
    // TODO Document.
    public Map<String, String> prefixes = new HashMap<String, String>();
    // TODO Document.
    public boolean hasElement;
    // TODO Document.
    public boolean skip;
    // TODO Document.
    public boolean choose;
    // TODO Document.
    public Ilk.Box context;
    // TODO Document.
    public Ilk.Box selected;
}
