package com.goodworkalan.stencil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

// TODO Document.
public class Page {
    // TODO Document.
    public final List<Object> nodes;
    // TODO Document.
    public final List<Integer> includes = new ArrayList<Integer>();
    // TODO Document.
    public final List<Integer> imports = new ArrayList<Integer>();
    // TODO Document.
    public final Map<QName, Integer> stencils = new HashMap<QName, Integer>();
    // TODO Document.
    public Page(List<Object> nodes) {
        this.nodes = nodes;
    }
}