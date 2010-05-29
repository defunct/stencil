package com.goodworkalan.stencil;

import org.xml.sax.Attributes;

public class Element {
    public final boolean start;
    
    public final int line;
    
    public final String localName;
    
    public final String namespaceURI;
    
    public final Attributes attributes;
    
//    public String getAttribute(String name, String namespaceURI) {
//        for (int i = 0; i < attributes.getLength(); i++) {
//            if (attributes.getURI(i).equals(namespaceURI) && attributes.getLocalName(i).equals(name)) {
//                return attributes.getValue(i); 
//            }
//        }
//        return null;
//    }
//    
//    public String getAttribute(String localName) {
//        return getAttribute(localName, "");
//    }

    public Element(boolean start, int line, String localName, String namespaceURI, Attributes attributes) {
        this.start = start;
        this.line = line;
        this.localName = localName;
        this.namespaceURI = namespaceURI;
        this.attributes = attributes;
    }
}
