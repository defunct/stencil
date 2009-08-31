package com.agtrz.stencil;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.apache.xerces.jaxp.SAXParserFactoryImpl;
import org.xml.sax.SAXException;

import com.habitsoft.xhtml.dtds.FailingEntityResolver;
import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

public class StencilSAXParserFactory extends SAXParserFactoryImpl {
    public StencilSAXParserFactory() {
    }
    
    @Override
    public SAXParser newSAXParser() throws ParserConfigurationException {
        SAXParser parser = super.newSAXParser();
        try {
            parser.getXMLReader().setEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return parser;
    }
}
