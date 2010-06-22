package com.goodworkalan.stencil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLReaderFactory;

import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

/**
 * Models an XML document as a series of events, capturing SAX events and
 * turning them into an array of event objects.
 * 
 * @author Alan Gutierrez
 */
class InOrderDocument implements ContentHandler, DTDHandler, LexicalHandler {
    /** Whether or not we are in a Document Type Definition. */
    boolean inDTD;
    
    // TODO Document.
    public static List<Object> readInOrderDocument(InputStream in)
    throws IOException, SAXException {
        List<Object> nodes = new ArrayList<Object>();
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(new XhtmlEntityResolver());     
        InOrderDocument iod = new InOrderDocument(nodes);
        reader.setContentHandler(iod);
        reader.setDTDHandler(iod);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", iod);
        reader.parse(new InputSource(in));
        return nodes;
    }
    
    // TODO Document.
    private final StringBuilder characters = new StringBuilder();
    
    // TODO Document.
    private Locator locator;
    
    // TODO Document.
    private final List<Object> nodes;
    
    // TODO Document.
    public InOrderDocument(List<Object> nodes) {
        this.nodes = nodes;
    }
    
    // TODO Document.
    public void startDocument() throws SAXException {
        nodes.clear();
    }
    
    // TODO Document.
    private void flushCharacters() {
        if (characters.length() != 0) {
            nodes.add(characters.toString());
            characters.setLength(0);
        }
    }
    
    // TODO Document.
    public void startPrefixMapping(String prefix, String uri)
    throws SAXException {
        flushCharacters();
        nodes.add(new String[] { prefix, uri });
    }
    
    // TODO Document.
    public void startElement(String uri, String localName, String qName, Attributes atts)
    throws SAXException {
        flushCharacters();
        nodes.add(new Element(true, locator.getLineNumber(), localName, uri, new AttributesImpl(atts)));
    }

    // TODO Document.
    public void characters(char[] ch, int start, int length)
    throws SAXException {
        characters.append(ch, start, length);
    }
    
    // TODO Document.
    public void ignorableWhitespace(char[] ch, int start, int length)
    throws SAXException {
        characters.append(ch, start, length);
    }
    
    // TODO Document.
    public void processingInstruction(String target, String data)
    throws SAXException {
    }
    
    // TODO Document.
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
    
    // TODO Document.
    public void skippedEntity(String name) throws SAXException {
    }
    
    // TODO Document.
    public void endElement(String uri, String localName, String qName)
    throws SAXException {
        flushCharacters();
        nodes.add(new Element(false, locator.getLineNumber(), localName, uri, new AttributesImpl()));
    }
    
    // TODO Document.
    public void endPrefixMapping(String prefix) throws SAXException {
        flushCharacters();
        nodes.add(Arrays.asList(prefix));
    }
    
    // TODO Document.
    public void endDocument() throws SAXException {
        flushCharacters();
    }
    
    // DTDHandler
    // TODO Document.
    public void notationDecl(String name, String publicId, String systemId)
    throws SAXException {
//        nodes.add(new NotationDeclaration(name, publicId, systemId));
    }

    // TODO Document.
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
    }
    
    // TODO Document.
    public void startDTD(String name, String publicId, String systemId)
    throws SAXException {
        nodes.add(new DocumentTypeDefinition(true, name, publicId, systemId));
        inDTD = true;
    }
    
    /** Does nothing since we do not care if text came to us in a CDATA section. */
    public void startCDATA() throws SAXException {
    }
    
    /** Does nothing since we do not care if text came to us in a CDATA section. */
    public void endCDATA() throws SAXException {
    }
    
    // TODO Document.
    public void endDTD() throws SAXException {
        nodes.add(new DocumentTypeDefinition(false, null, null, null));
        inDTD = false;
    }
    
    // TODO Document.
    public void startEntity(String name) throws SAXException {
    }
    
    // TODO Document.
    public void endEntity(String name) throws SAXException {
    }
    
    // TODO Document.
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (!inDTD) {
            nodes.add(new Comment(new String(ch, start, length)));
        }
    }
}
