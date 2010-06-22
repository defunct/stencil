package com.goodworkalan.stencil;

import org.xml.sax.Attributes;

/**
 * The start or end of an element in an XML document.
 *
 * @author Alan Gutierrez
 */
class Element {
    /** Whether or not this is the start of the element. */
    public final boolean start;
    
    /** The line number where the element started or ended. */
    public final int line;
    
    /** The local name of the element. */
    public final String localName;
    
    /** The namespace URI of the element. */
    public final String namespaceURI;
    
    /** The element attributes. */
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

    /**
     * Create a new XML element start or end.
     * 
     * @param start
     *            Whether or not this is the start of the element.
     * @param line
     *            The line number where the element started or ended.
     * @param localName
     *            The local name of the element.
     * @param namespaceURI
     *            The namespace URI of the element.
     * @param attributes
     *            The element attributes.
     */
    public Element(boolean start, int line, String localName, String namespaceURI, Attributes attributes) {
        this.start = start;
        this.line = line;
        this.localName = localName;
        this.namespaceURI = namespaceURI;
        this.attributes = attributes;
    }
}
