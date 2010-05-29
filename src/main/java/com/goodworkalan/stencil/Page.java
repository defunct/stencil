package com.goodworkalan.stencil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

public class Page {
    public final List<Object> nodes;
    public final List<Integer> includes = new ArrayList<Integer>();
    public final List<Integer> imports = new ArrayList<Integer>();
    public final Map<QName, Integer> stencils = new HashMap<QName, Integer>();
    public Page(List<Object> nodes) {
        this.nodes = nodes;
    }
}