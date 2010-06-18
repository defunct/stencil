package com.goodworkalan.stencil;

import java.util.HashMap;
import java.util.Map;

import com.goodworkalan.ilk.Ilk;

public class Level {
    public Map<String, String> namespaceURIs = new HashMap<String, String>();
    public Map<String, String> prefixes = new HashMap<String, String>();
    public boolean hasElement;
    public boolean skip;
    public boolean choose;
    public Ilk.Box context;
    public Ilk.Box selected;
}
