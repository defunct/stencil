package com.goodworkalan.stencil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

class InOrderDocument implements ContentHandler {
    public static List<Object> readInOrderDocument(InputStream in) throws IOException, SAXException {
        List<Object> nodes = new ArrayList<Object>();
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(new XhtmlEntityResolver());          
        reader.setContentHandler(new InOrderDocument(nodes));
        reader.parse(new InputSource(in));
        return nodes;
    }
    
    private final StringBuilder characters = new StringBuilder();
    
    private Locator locator;
    
    private final List<Object> nodes;
    
    public InOrderDocument(List<Object> nodes) {
        this.nodes = nodes;
    }
    
    public void startDocument() throws SAXException {
        nodes.clear();
    }
    
    private void flushCharacters() {
        if (characters.length() != 0) {
            nodes.add(characters.toString());
            characters.setLength(0);
        }
    }
    
    public void startPrefixMapping(String prefix, String uri)
    throws SAXException {
        flushCharacters();
        nodes.add(new String[] { prefix, uri });
    }
    
    public void startElement(String uri, String localName, String qName, Attributes atts)
    throws SAXException {
        flushCharacters();
        nodes.add(new Element(true, locator.getLineNumber(), localName, uri, new AttributesImpl(atts)));
    }

    public void characters(char[] ch, int start, int length)
    throws SAXException {
        characters.append(ch, start, length);
    }
    
    public void ignorableWhitespace(char[] ch, int start, int length)
    throws SAXException {
        characters.append(ch, start, length);
    }
    
    public void processingInstruction(String target, String data)
    throws SAXException {
    }
    
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
    
    public void skippedEntity(String name) throws SAXException {
    }
    
    public void endElement(String uri, String localName, String qName)
    throws SAXException {
        flushCharacters();
        nodes.add(new Element(false, locator.getLineNumber(), localName, uri, new AttributesImpl()));
    }
    
    public void endPrefixMapping(String prefix) throws SAXException {
        flushCharacters();
        nodes.add(Arrays.asList(prefix));
    }
    
    public void endDocument() throws SAXException {
        flushCharacters();
    }
}
