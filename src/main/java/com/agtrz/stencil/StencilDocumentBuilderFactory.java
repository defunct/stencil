package com.agtrz.stencil;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;

import com.habitsoft.xhtml.dtds.FailingEntityResolver;
import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

public class StencilDocumentBuilderFactory extends DocumentBuilderFactoryImpl {
    public StencilDocumentBuilderFactory() {
        
    }
    
    @Override
    public DocumentBuilder newDocumentBuilder()
            throws ParserConfigurationException {
        DocumentBuilder builder = super.newDocumentBuilder();
        builder.setEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        return builder;
    }
}
