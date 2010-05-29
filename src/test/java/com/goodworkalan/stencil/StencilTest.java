/* Copyright Alan Gutierrez 2006 */
package com.goodworkalan.stencil;

import java.beans.IntrospectionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.apache.xerces.parsers.SAXParser;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.InjectorBuilder;
import com.habitsoft.xhtml.dtds.FailingEntityResolver;
import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

public class StencilTest extends XMLTestCase {
    public TransformerHandler foo(Result result) throws TransformerConfigurationException, TransformerFactoryConfigurationError {
        TransformerHandler handler = ((SAXTransformerFactory) SAXTransformerFactory.newInstance()).newTransformerHandler();

//        handler.setAttribute("indent-number", new Integer(2));

//        Transformer xformer = handler.newTransformer();
//        xformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        xformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
 
        handler.setResult(result);
        return handler;
    }
    
    @Test
    public void testDocument()
    throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        URL url = Thread.currentThread().getContextClassLoader().getResource("org/jaxen/BaseXPath.class");
        
        System.out.println(url);
        
        new XhtmlEntityResolver(new FailingEntityResolver());
        
        Stencil.Template template = new Stencil.Template(getClass().getResourceAsStream("var.xhtml"));
        Stencil.Snippit snippit = template.newSnippit("hello");
        final Person person = new Person("Steve", "McQueen");

        snippit.bind(person);

        // new Serializer(System.out).write(snippit.getDocument());

        Builder builder = new Builder();
        Document control = builder.build(getClass().getResourceAsStream("var.out.xhtml"));
        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
        
        StencilFactory stencils = new StencilFactory();
        
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(person, ilk(Person.class), null);
            }
        });
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        stencils.setInjector(injector);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = foo(stream);
        stencils.stencil(URI.create("var.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/var.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }
    
    public String slurp(InputStream in) throws IOException {
        StringBuilder string = new StringBuilder();
        Reader reader = new InputStreamReader(in);
        int ch;
        while ((ch = reader.read()) != -1) {
            string.append((char) ch);
        }
        return string.toString();
    }

    public org.w3c.dom.Document load(InputStream in, String uri) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(in, uri);
    }
    

    @Test
    public void testEach() throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException
    {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        Stencil.Template template = new Stencil.Template(getClass().getResourceAsStream("each.xhtml"));
        Stencil.Snippit snippit = template.newSnippit("hello");

        List listOfPeople = new ArrayList();

        listOfPeople.add(new Person("Betty", "Rubble"));
        listOfPeople.add(new Person("Barney", "Rubble"));
        listOfPeople.add(new Person("Wilma", "Flintstone"));
        listOfPeople.add(new Person("Fred", "Flintstone"));

        Map mapOfBindings = new HashMap();
        mapOfBindings.put("people", listOfPeople);

        snippit.bind(mapOfBindings);

        // new Serializer(System.out).write(snippit.getDocument());

        Document control = getControl("each.out.xhtml");
//        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

    @Test
    public void testDefault() throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException
    {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        Stencil.Template template = new Stencil.Template(getClass().getResourceAsStream("default.xhtml"));
        Stencil.Snippit snippit = template.newSnippit("hello");

        snippit.bind(Collections.EMPTY_MAP);

        // new Serializer(System.out).write(snippit.getDocument());

        Document control = getControl("default.out.xhtml");
//        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

    @Test
    public void testIf() throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException
    {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        Stencil.Template template = new Stencil.Template(getClass().getResourceAsStream("if.xhtml"));
        Stencil.Snippit snippit = template.newSnippit("hello");

        Map mapOfBindings = new HashMap();

        mapOfBindings.put("barney", new Person("Betty", "Rubble"));

        snippit.bind(mapOfBindings);

        new Serializer(System.out).write(snippit.getDocument());

        Document control = getControl("if.out.xhtml");
//        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

    @Test
    private Document getControl(String file) throws ParsingException, ValidityException,
            IOException {
        SAXParser parser = new SAXParser();
        parser.setEntityResolver(new XhtmlEntityResolver());          
        Builder builder = new Builder(parser, false);
        Document control = builder.build(getClass().getResourceAsStream(file));
        return control;
    }

    @Test
    public void testUnless() throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException
    {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        Stencil.Template template = new Stencil.Template(getClass().getResourceAsStream("unless.xhtml"));
        Stencil.Snippit snippit = template.newSnippit("hello");

        Map mapOfBindings = new HashMap();

        mapOfBindings.put("barney", new Person("Betty", "Rubble"));

        snippit.bind(mapOfBindings);

        // new Serializer(System.out).write(snippit.getDocument());

        Document control = getControl("unless.out.xhtml");
//        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

    @Test
    public void testInvoke() throws ValidityException, ParsingException, IOException, SAXException, ParserConfigurationException
    {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        Stencil.Template outer = new Stencil.Template(getClass().getResourceAsStream("outer.xhtml"));
        Stencil.Template inner = new Stencil.Template(getClass().getResourceAsStream("inner.xhtml"));

        Stencil.Generator generator = new Stencil.Generator();

        generator.addTemplate(outer);
        generator.addTemplate(inner);

        Map mapOfBindings = new HashMap();

        mapOfBindings.put("betty", new Person("Betty", "Rubble"));

        // new
        // Serializer(System.out).write(generator.bind("outer",mapOfBindings));

        Document document = generator.bind("outer", mapOfBindings);

        Document control = getControl("invoke.out.xhtml");
//        assertXMLEqual(control.toXML(), document.toXML());
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */