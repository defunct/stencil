/* Copyright Alan Gutierrez 2006 */
package com.agtrz.stencil;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.apache.xerces.parsers.SAXParser;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.xml.sax.SAXException;

import com.habitsoft.xhtml.dtds.FailingEntityResolver;
import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

public class StencilTestCase
extends XMLTestCase
{
    public void testDocument() throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException
    {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        URL url = Thread.currentThread().getContextClassLoader().getResource("org/jaxen/BaseXPath.class");
        
        System.out.println(url);
        
        new XhtmlEntityResolver(new FailingEntityResolver());
        
        Stencil.Template template = new Stencil.Template(getClass().getResourceAsStream("var.xhtml"));
        Stencil.Snippit snippit = template.newSnippit("hello");
        Person person = new Person("Barney", "Rubble");

        snippit.bind(person);

        // new Serializer(System.out).write(snippit.getDocument());

        Builder builder = new Builder();
        Document control = builder.build(getClass().getResourceAsStream("var.out.xhtml"));
        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

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
        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

    public void testDefault() throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException
    {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        Stencil.Template template = new Stencil.Template(getClass().getResourceAsStream("default.xhtml"));
        Stencil.Snippit snippit = template.newSnippit("hello");

        snippit.bind(Collections.EMPTY_MAP);

        // new Serializer(System.out).write(snippit.getDocument());

        Document control = getControl("default.out.xhtml");
        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

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
        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

    private Document getControl(String file) throws ParsingException, ValidityException,
            IOException {
        SAXParser parser = new SAXParser();
        parser.setEntityResolver(new XhtmlEntityResolver());          
        Builder builder = new Builder(parser, false);
        Document control = builder.build(getClass().getResourceAsStream(file));
        return control;
    }

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
        assertXMLEqual(control.toXML(), snippit.getDocument().toXML());
    }

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
        assertXMLEqual(control.toXML(), document.toXML());
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */