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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import nu.xom.ParsingException;
import nu.xom.ValidityException;

import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.InjectorBuilder;

public class StencilTest extends XMLTestCase {
    public TransformerHandler foo(Result result) throws TransformerConfigurationException, TransformerFactoryConfigurationError {
        TransformerHandler handler = ((SAXTransformerFactory) TransformerFactory.newInstance()).newTransformerHandler();
        handler.setResult(result);
        return handler;
    }
    
    @Test
    public void testDocument()
    throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

        final Person person = new Person("Steve", "McQueen");

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
    public void testEach() throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError {
        XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
        XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());

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
        stencils.setInjector(injector);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = foo(stream);
        stencils.stencil(URI.create("each.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/each.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }

    @Test
    public void testIf()
    throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError {
    	XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
    	XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());
	
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
        stencils.setInjector(injector);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = foo(stream);
        stencils.stencil(URI.create("if.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/if.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }

	@Test
    public void testDefault()
	throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError {
    	XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
    	XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());
	
    	InjectorBuilder newInjector = new InjectorBuilder();
    	newInjector.module(new InjectorBuilder() {
    		protected void build() {
    			instance(new Person(null, null), ilk(Person.class), null);
    		}
    	});
    
    	StencilFactory stencils = new StencilFactory();
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        stencils.setInjector(injector);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = foo(stream);
        stencils.stencil(URI.create("default.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/default.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }

    @Test
    public void testUnless()
    throws ValidityException, ParsingException, IOException, IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SAXException, ParserConfigurationException, TransformerConfigurationException, TransformerFactoryConfigurationError {
    	XMLUnit.setControlParser(StencilDocumentBuilderFactory.class.getCanonicalName());
    	XMLUnit.setTestParser(StencilDocumentBuilderFactory.class.getCanonicalName());
	
    	InjectorBuilder newInjector = new InjectorBuilder();
    	newInjector.module(new InjectorBuilder() {
    		protected void build() {
    			instance(new Person(null, null), ilk(Person.class), null);
    		}
    	});
    
    	StencilFactory stencils = new StencilFactory();
        Injector injector = newInjector.newInjector();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil/test").getAbsoluteFile().toURI());
        stencils.setInjector(injector);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamResult stream = new StreamResult(out);
        TransformerHandler handler = foo(stream);
        stencils.stencil(URI.create("unless.xhtml"), handler);
        String control1 = slurp(getClass().getResourceAsStream("test/unless.out.xhtml"));
        String actual = slurp(new ByteArrayInputStream(out.toByteArray()));
        assertXMLEqual(control1, actual);
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */