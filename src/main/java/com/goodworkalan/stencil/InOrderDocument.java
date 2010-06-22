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
    /** The list of nodes. */
    private final List<Object> events;

    /** The character buffer. */
    private final StringBuilder characters = new StringBuilder();

    /** The document locator used to get element line numbers. */
    private Locator locator;
    
    /** Whether or not we are in a Document Type Definition. */
    private boolean inDTD;

    /**
     * Create an in order XML document that records events to the given list of
     * events.
     * 
     * @param events
     *            The list of events.
     */
    public InOrderDocument(List<Object> events) {
        this.events = events;
    }
    
    /**
     * Read an XML document from the given input stream as an in order document
     * returning a list event objects.
     * 
     * @param in
     *            The input stream.
     * @return A list of event objects.
     * @throws IOException
     *             For any I/O error.
     * @throws SAXException
     *             For any reason, any reason at all.
     */
    public static List<Object> readInOrderDocument(InputStream in)
    throws IOException, SAXException {
        List<Object> events = new ArrayList<Object>();
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(new XhtmlEntityResolver());     
        InOrderDocument iod = new InOrderDocument(events);
        reader.setContentHandler(iod);
        reader.setDTDHandler(iod);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", iod);
        reader.parse(new InputSource(in));
        return events;
    }

    /** Start the XML document. */
    public void startDocument() {
        events.clear();
    }

    /**
     * Flush the character buffer by adding the character buffer content to the
     * list of events as a <code>String</code> and resetting the buffer. If the
     * buffer is currently empty, do nothing.
     */
    private void flushCharacters() {
        if (characters.length() != 0) {
            events.add(characters.toString());
            characters.setLength(0);
        }
    }

    /**
     * Record a namespace prefix mapping in the list of events as a two element
     * string array where there prefix is the first element and the namespace
     * uri is the second element. Prior to adding the prefix mapping event, the
     * character buffer is flushed and any gathered characters are added to the
     * event is as a <code>String</code>.
     * 
     * @param prefix
     *            The prefix.
     * @param uri
     *            The namespace URI.
     */
    public void startPrefixMapping(String prefix, String uri) {
        flushCharacters();
        events.add(new String[] { prefix, uri });
    }

    /**
     * Record a the start of an element by adding an <code>Element</code>
     * instance to the list of events. Prior to adding the start element event, the
     * character buffer is flushed and any gathered characters are added to the
     * event is as a <code>String</code>. 
     * 
     * @param uri
     *            The namespace URI.
     * @param localName
     *            The local name.
     * @param qName
     *            The qualified name.
     * @param atts
     *            The attributes.
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) {
        flushCharacters();
        events.add(new Element(true, locator.getLineNumber(), localName, uri, new AttributesImpl(atts)));
    }

    /**
     * Gather the parser characters into a character buffer for subsequent
     * insertion into the list of events.
     * 
     * @param ch
     *            The SAX parser character buffer.
     * @param start
     *            The offset into the SAX parser character buffer.
     * @param length
     *            The length of the valid region in the SAX parser character
     *            buffer.
     */
    public void characters(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }
    
    /**
     * Gather the parser characters into a character buffer for subsequent
     * insertion into the list of events.
     * 
     * @param ch
     *            The SAX parser character buffer.
     * @param start
     *            The offset into the SAX parser character buffer.
     * @param length
     *            The length of the valid region in the SAX parser character
     *            buffer.
     */
    public void ignorableWhitespace(char[] ch, int start, int length) {
        characters.append(ch, start, length);
    }

    /**
     * Processing instructions are ignored.
     * 
     * @param target
     *            The target.
     * @param data
     *            The data.
     */
    public void processingInstruction(String target, String data) {
    }

    /**
     * Set the XML parser document locator which is used to record the line
     * numbers of start and end elements.
     * 
     * @param locator
     *            The XML paraser document locator.
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    /**
     * Skipped entities are ignored.
     * 
     * @param name
     *            The entity name.
     */
    public void skippedEntity(String name) {
    }

    /**
     * Record the end of an element by adding an <code>Element</code> instance
     * to the list of events. Prior to adding the end element event, the
     * character buffer is flushed and any gathered characters are added to the
     * event is as a <code>String</code>.
     * 
     * @param uri
     *            The namespace URI.
     * @param localName
     *            The local name.
     * @param qName
     *            The qualified name.
     */
    public void endElement(String uri, String localName, String qName) {
        flushCharacters();
        events.add(new Element(false, locator.getLineNumber(), localName, uri, new AttributesImpl()));
    }

    /**
     * Record the end of a prefix mapping as an array with a single element that
     * is the prefix. Prior to adding the end prefix mapping event, the
     * character buffer is flushed and any gathered characters are added to the
     * event list as a <code>String</code>.
     * 
     * @param prefix
     *            The prefix.
     */
    public void endPrefixMapping(String prefix) {
        flushCharacters();
        events.add(Arrays.asList(prefix));
    }
    
    /**
     * The the XML document, flushing the character buffer and adding any
     * gathered characters to the event list as a <code>String</code>.
     */
    public void endDocument() {
        flushCharacters();
    }
    
    // DTDHandler

    /**
     * Notation declarations are ignored.
     * 
     * @param name
     *            The name.
     * @param publicId
     *            The public id.
     * @param systemId
     *            The system id.
     */
    public void notationDecl(String name, String publicId, String systemId) {
//        nodes.add(new NotationDeclaration(name, publicId, systemId));
    }

    /**
     * Unparsed entity declarations are currently ignored.
     * 
     * @param name
     *            The name.
     * @param publicId
     *            The public id.
     * @param systemId
     *            The system id.
     * @param notationName
     *            The notation name.
     */
    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) {
    }

    /**
     * Record the start of a DTD by adding a <code>DocumentTypeDefinition</code>
     * instance to the list of events.
     * 
     * @param name
     *            The name.
     * @param publicId
     *            The public id.
     * @param systemId
     *            The system id.
     */
    public void startDTD(String name, String publicId, String systemId) {
        events.add(new DocumentTypeDefinition(true, name, publicId, systemId));
        inDTD = true;
    }
    
    /** Does nothing since we do not care if text came to us in a CDATA section. */
    public void startCDATA() {
    }
    
    /** Does nothing since we do not care if text came to us in a CDATA section. */
    public void endCDATA() {
    }
    
    /**
     * Record the start of a DTD by adding a <code>DocumentTypeDefinition</code>
     * instance to the list of events.
     */
    public void endDTD() {
        events.add(new DocumentTypeDefinition(false, null, null, null));
        inDTD = false;
    }
    
    /**
     * Entities are ignored.
     * 
     * @param name The entity name.
     */
    public void startEntity(String name) {
    }
    
    /**
     * Entities are ignored.
     * 
     * @param name The entity name.
     */
    public void endEntity(String name) {
    }

    /**
     * Record a comment by adding a <code>Comment</code> instance to the list of
     * events using the given range of the SAX parser character buffer as the
     * comment content.
     * 
     * @param ch
     *            The SAX parser character buffer.
     * @param start
     *            The offset into the SAX parser character buffer.
     * @param length
     *            The length of the valid region in the SAX parser character
     *            buffer.
     */
    public void comment(char[] ch, int start, int length) throws SAXException {
        if (!inDTD) {
            flushCharacters();
            events.add(new Comment(new String(ch, start, length)));
        }
    }
}
