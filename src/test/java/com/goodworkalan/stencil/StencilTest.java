/* Copyright Alan Gutierrez 2006 */
package com.goodworkalan.stencil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;

import org.testng.annotations.Test;

import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.InjectorBuilder;

import static org.testng.Assert.*;


/**
 * Unit tests for the {@link StencilFactory} class.
 *
 * @author Alan Gutierrez
 */
public class StencilTest {
    /** Test a do nothing template. */
//    @Test
    public void nothing() throws IOException {
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        StringWriter output = new StringWriter();
        stencils.stencil(new InjectorBuilder().newInjector(), URI.create("nothing.txt"), output);
        String control = slurp(getClass().getResourceAsStream("nothing.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test variable assignments. */
    @Test
    public void get() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("get.txt"), output);
        String control = slurp(getClass().getResourceAsStream("get.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
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
//
//    /** Test each. */
//    @Test
//    public void testEach() throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
//        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
//        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
//
//        final Clique clique = new Clique();
//        
//        clique.people.add(new Person("George", "Washington"));
//        clique.people.add(new Person("John", "Adams"));
//        clique.people.add(new Person("Thomas", "Jefferson"));
//        clique.people.add(new Person("James", "Madison"));
//
//        InjectorBuilder newInjector = new InjectorBuilder();
//        newInjector.module(new InjectorBuilder() {
//            protected void build() {
//                instance(clique, ilk(Clique.class), null);
//            }
//        });
//
//        StencilFactory stencils = new StencilFactory();
//        Injector injector = newInjector.newInjector();
//        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        StreamResult stream = new StreamResult(out);
//        TransformerHandler handler = newTransformerHandler(stream);
//        stencils.stencil(injector, URI.create("each.xhtml"), handler);
//        String control1 = slurp(getClass().getResourceAsStream("test/each.out.xhtml"));
//        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
//        assertXMLEqual(control1, actual);
//    }

    /** Test if. */
    @Test
    public void testIf() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test inline if with content after the closing if. */
    @Test
    public void ifTrailing() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-trailing.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-trailing.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }
    
    /** Test inline if with content before the opening if. */
    @Test
    public void ifLeading() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-leading.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-leading.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test inline if with false value for if. */
    @Test
    public void ifFalse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-false.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-false.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }


//    /** Test default. */
//    @Test
//    public void testDefault()
//    throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
//        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
//        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
//
//        InjectorBuilder newInjector = new InjectorBuilder();
//        newInjector.module(new InjectorBuilder() {
//            protected void build() {
//                instance(new Person(null, null), ilk(Person.class), null);
//            }
//        });
//    
//        StencilFactory stencils = new StencilFactory();
//        Injector injector = newInjector.newInjector();
//        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        StreamResult stream = new StreamResult(out);
//        TransformerHandler handler = newTransformerHandler(stream);
//        stencils.stencil(injector, URI.create("default.xhtml"), handler);
//        String control1 = slurp(getClass().getResourceAsStream("test/default.out.xhtml"));
//        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
//        assertXMLEqual(control1, actual);
//    }
//
//    /** Test unless. */
//    @Test
//    public void testUnless()
//    throws IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException {
//        XMLUnit.setControlEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
//        XMLUnit.setTestEntityResolver(new XhtmlEntityResolver(new FailingEntityResolver()));
//    
//        InjectorBuilder newInjector = new InjectorBuilder();
//        newInjector.module(new InjectorBuilder() {
//            protected void build() {
//                instance(new Person(null, null), ilk(Person.class), null);
//            }
//        });
//    
//        StencilFactory stencils = new StencilFactory();
//        Injector injector = newInjector.newInjector();
//        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        StreamResult stream = new StreamResult(out);
//        TransformerHandler handler = newTransformerHandler(stream);
//        stencils.stencil(injector, URI.create("unless.xhtml"), handler);
//        String control1 = slurp(getClass().getResourceAsStream("test/unless.out.xhtml"));
//        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
//        assertXMLEqual(control1, actual);
//    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */
