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
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.InjectorBuilder;
import com.habitsoft.xhtml.dtds.FailingEntityResolver;
import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

/**
 * Unit tests for the {@link StencilFactory} class.
 *
 * @author Alan Gutierrez
 */
public class StencilTest extends XMLTestCase {
    /**
     * Create a new transformer handler that records to the given result.
     * 
     * @param result
     *            The result.
     * @return A new transformer handler.
     * @throws TransformerConfigurationException
     *             If for some reason the TransformerHandler cannot be created.
     */
    public TransformerHandler newTransformerHandler(Result result) throws TransformerConfigurationException {
        TransformerHandler handler = ((SAXTransformerFactory) TransformerFactory.newInstance()).newTransformerHandler();
        handler.setResult(result);
        return handler;
    }
    
    /** Test variable assignments. */
    @Test
    public void var()
    throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));

        StencilFactory stencils = new StencilFactory();
        
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("Steve", "McQueen"), ilk(Person.class), null);
            }
        });
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = newTransformerHandler(stream);
        stencils.stencil(injector, URI.create("var.xhtml"), handler);
        String control = slurp(getClass().getResourceAsStream("test/var.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control, actual);
    }

    /**
     * Slurp the entire input stream into a single string.
     * 
     * @param in
     *            The input stream.
     * @return The contents of the stream as a string.
     * @throws IOException
     *             For any I/O error.
     */
    public String slurp(InputStream in) throws IOException {
        StringBuilder string = new StringBuilder();
        Reader reader = new InputStreamReader(in);
        int ch;
        while ((ch = reader.read()) != -1) {
            string.append((char) ch);
        }
        return string.toString();
    }

    /** Test each. */
    @Test
    public void testEach() throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));

        final Clique clique = new Clique();
        
        clique.people.add(new Person("George", "Washington"));
        clique.people.add(new Person("John", "Adams"));
        clique.people.add(new Person("Thomas", "Jefferson"));
        clique.people.add(new Person("James", "Madison"));

        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(clique, ilk(Clique.class), null);
            }
        });

        StencilFactory stencils = new StencilFactory();
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = newTransformerHandler(stream);
        stencils.stencil(injector, URI.create("each.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/each.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }

    /** Test if. */
    @Test
    public void testIf()
    throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
    
        final Map<String, Person> people = new HashMap<String, Person>();
        people.put("second", new Person("George", "Washington"));
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(people, new Ilk<Map<String, Person>>() {}, null);
            }
        });
    
        StencilFactory stencils = new StencilFactory();
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = newTransformerHandler(stream);
        stencils.stencil(injector, URI.create("if.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/if.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }

    /** Test default. */
    @Test
    public void testDefault()
    throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));

        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, null), ilk(Person.class), null);
            }
        });
    
        StencilFactory stencils = new StencilFactory();
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = newTransformerHandler(stream);
        stencils.stencil(injector, URI.create("default.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/default.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }

    /** Test unless. */
    @Test
    public void testUnless()
    throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
    
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, null), ilk(Person.class), null);
            }
        });
    
        StencilFactory stencils = new StencilFactory();
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = newTransformerHandler(stream);
        stencils.stencil(injector, URI.create("unless.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/unless.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */
