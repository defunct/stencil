package com.goodworkalan.stencil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.habitsoft.xhtml.dtds.FailingEntityResolver;
import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

public class StencilDocumentBuilderFactory
extends DocumentBuilderFactory {
    private final DocumentBuilderFactory dbf;

    public StencilDocumentBuilderFactory() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
    }
    
    @Override
    public DocumentBuilder newDocumentBuilder()
    throws ParserConfigurationException {
        DocumentBuilder builder = dbf.newDocumentBuilder();
        builder.setEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        return builder;
    }
    
    @Override
    public Object getAttribute(String name) {
        return dbf.getAttribute(name);
    }
    
    @Override
    public void setAttribute(String name, Object value) {
        dbf.setAttribute(name, value);
    }
    
    @Override
    public boolean getFeature(String name) throws ParserConfigurationException {
        return dbf.getFeature(name);
    }
    
    @Override
    public void setFeature(String name, boolean value) throws ParserConfigurationException {
        dbf.setFeature(name, value);
    }
}
