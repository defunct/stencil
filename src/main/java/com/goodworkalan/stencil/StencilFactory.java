package com.goodworkalan.stencil;

import static com.goodworkalan.ilk.Types.getActualType;
import static com.goodworkalan.ilk.Types.getRawClass;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;
import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;

import com.goodworkalan.diffuse.Diffuser;
import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.loader.IlkLoader;
import com.goodworkalan.permeate.ParseException;
import com.goodworkalan.permeate.Part;
import com.goodworkalan.permeate.Path;
import com.goodworkalan.reflective.ReflectiveException;
import com.goodworkalan.reflective.getter.Getter;
import com.goodworkalan.reflective.getter.Getters;

// TODO Document.
public class StencilFactory {
    /** The Stencil name space URI. */
    public final static String STENCIL_URI = "http://stencil.goodworkalan:2010/stencil";
    
    /** The base URI from which resource URIs are resolved. */
    private URI baseURI;
    
    /** The map of protocols to resource resolvers. */
    private final Map<String, ResourceResolver> resourceResolvers = new ConcurrentHashMap<String, ResourceResolver>();

    private static final Diffuser diffuser = new Diffuser();
    
    /** The cache of compiled and verified stencils. */
    private final ConcurrentMap<URI, Page> stencils = new ConcurrentHashMap<URI, Page>();
    
    /**
     * Add a resource resolver for the given protocol scheme. The protocol
     * scheme is the first part of a full URL, such as <code>http</code>,
     * <code>ftp</code>, or <code>file</code>. This <code>StencilFactory</code>
     * will use the given resource resolver to obtain a URL from a URI when the
     * URI is that of the given protocol.
     * 
     * @param scheme
     *            The protocol.
     * @param resourceResolver
     *            The resource resolver.
     */
    public void addResolver(String scheme, ResourceResolver resourceResolver) {
        resourceResolvers.put(scheme, resourceResolver);
    }

    // TODO Document.
    public synchronized URI getBaseURI() {
        return baseURI;
    }

    // TODO Document.
    public synchronized void setBaseURI(URI uri) {
        this.baseURI = uri;
    }
    
    // TODO Document.
    protected InputStream getInputStream(URI uri) {
        try {
            return uri.toURL().openStream();
        } catch (IOException e) {
            throw new StencilException(e, "Unable to open stencil [%s].", uri);
        }
    }

    // TODO Document.
    public void stencil(Injector injector, URI uri, TransformerHandler handler) throws SAXException {
        stencil(injector, uri, handler, handler, handler);
    }

    // TODO Document.
    public void stencil(Injector injector, URI uri, ContentHandler content, LexicalHandler lexical, DTDHandler dtd) throws SAXException {
        URI resolved = baseURI.resolve(uri).normalize();
        Page page = stencils.get(resolved);
        if (page == null) {
            page = compile2(injector, uri, content, lexical, dtd);
            stencils.put(resolved, page);
        } else {
            compile(injector, uri, page.nodes, 0, new Page(new ArrayList<Object>()), content, lexical, dtd);
        }
    }
    
    // TODO Document.
    private static String getNamespace(LinkedList<Level> stack, String prefix) {
        ListIterator<Level> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level level = iterator.previous();
            String uri = level.namespaceURIs.get(prefix);
            if (uri != null) {
                return uri;
            }
        }
        return null;
    }
    
    // TODO Document.
    private Object getSelected(LinkedList<Level> stack) {
        ListIterator<Level> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level level = iterator.previous();
            if (level.selected != null) {
                return level.selected.object;
            }
        }
        return null;
    }

    // TODO Document.
    private Ilk.Box getContext(LinkedList<Level> stack) {
        ListIterator<Level> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level level = iterator.previous();
            if (level.context != null) {
                return level.context;
            }
        }
        return null;
    }
    
    // TODO Document.
    private boolean isBlank(String string) {
        return string == null || string.equals("");
    }
    
    // TODO Document.
    private URL getURL(Injector injector, URI uri) throws MalformedURLException {
        ResourceResolver resourceResolver = resourceResolvers.get(uri.getScheme());
        if (resourceResolver == null) {
            return uri.toURL();
        }
        return resourceResolver.getURL(injector, uri);
    }

    // TODO Document.
    private Page compile2(Injector injector, URI uri, ContentHandler output, LexicalHandler lexical, DTDHandler dtd) throws SAXException {
        // Normalize the absolute URI.
        uri = getBaseURI().resolve(uri).normalize();
        // Load the document.
        List<Object> nodes;
        try {
            URL url = getURL(injector, uri); 
            nodes = InOrderDocument.readInOrderDocument(url.openStream());
        } catch (SAXException e) {
            throw new StencilException(e, "Invalid XML in [%s].", uri);
        } catch (IOException e) {
            throw new StencilException(e, "Cannot read [%s].", uri);
        }
        
        return compile(injector, uri, nodes, 0, new Page(nodes), output, lexical, dtd);
    }

    // TODO Document.
    private <T> void foo(Injector injector, LinkedList<Level> stack, URI uri, List<Object> nodes, int offset, Page page, ContentHandler output, LexicalHandler lexical, DTDHandler dtd) throws SAXException {
        for (int index = offset, stop = nodes.size(), height = stack.size(); index < stop; index++) {
            Object object = nodes.get(index);
            if (object instanceof NotationDeclaration) {
                if (dtd != null) {
                    NotationDeclaration nd = (NotationDeclaration) object;
                    dtd.notationDecl(nd.name, nd.publicId, nd.systemId);
                }
            } else if (object instanceof Comment) {
                if (lexical != null) {
                    String text = ((Comment) object).text;
                    lexical.comment(text.toCharArray(), 0, text.length());
                }
            } else if (object instanceof DocumentTypeDefinition) {
                DocumentTypeDefinition doctype = (DocumentTypeDefinition) object;
                if (doctype.start) {
                    lexical.startDTD(doctype.name, doctype.publicId, doctype.systemId);
                } else {
                    lexical.endDTD();
                }
            } else if (object instanceof String[]) {
                if (!stack.getLast().skip) {
                    String[] mapping = (String[]) object;
                    if (stack.getLast().hasElement) {
                        stack.addLast(new Level());
                    }
                    stack.getLast().namespaceURIs.put(mapping[0], mapping[1]);
                    stack.getLast().prefixes.put(mapping[1], mapping[0]);
                    output.startPrefixMapping(mapping[0], mapping[1]);
                }
            } else if (object instanceof Element) {
                Element element = (Element) object;
                if (!element.start) {
                    if (height == stack.size()) {
                        break;
                    }
                    if (output != null && !stack.getLast().skip && !stack.getLast().choose) {
                        output.endElement(element.namespaceURI, element.localName, getQualifiedName(stack, element.namespaceURI, element.localName));
                    }
                    for (String prefix : stack.getLast().namespaceURIs.keySet()) {
                        output.endPrefixMapping(prefix);
                    }
                    stack.removeLast();
                    continue;
                }
                if (stack.getLast().hasElement) {
                    boolean skip = stack.getLast().skip;
                    stack.addLast(new Level());
                    stack.getLast().skip = skip;
                    stack.getLast().hasElement = skip;
                }
                if (stack.getLast().skip) {
                    continue;
                }
                AttributesImpl resolved = new AttributesImpl(element.attributes);
                stack.getLast().hasElement = true;
                String localName = element.localName;
                boolean inStencilNamespace = element.namespaceURI.equals(STENCIL_URI);
                {
                    String name = null;
                    if (inStencilNamespace && localName.equals("stencil")) {
                        stack.getLast().skip = true;
                        name = resolved.getLocalName(resolved.getIndex("name"));
                        if (name == null) {
                            throw new StencilException("");
                        }
                    } else  {
                        int attribute = resolved.getIndex(STENCIL_URI, "stencil");
                        if (attribute != -1) {
                            name = resolved.getLocalName(attribute);
                            resolved.removeAttribute(attribute);
                        }
                    }
                    if (name != null && !name.equals("")) {
                        String[] qualified = name.split(":");
                        page.stencils.put(new QName(getNamespace(stack, qualified[0]), qualified[1]), index);
                        break;
                    }
                }
                { // Check for a new evaluation context.
                    int attribute = resolved.getIndex(STENCIL_URI, "context");
                    if (attribute != -1) {
                        String context = resolved.getValue(attribute);
                        resolved.removeAttribute(attribute);
                        if (!isBlank(context)) {
                            try {
                                stack.getLast().context = IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), context, Collections.<String, Class<?>>emptyMap());
                            } catch (ClassNotFoundException e) {
                                throw new StencilException(e, "Cannot load type [%s] at line [%s] of [%s].", context, element.line, uri);
                            }
                        }
                        if (output != null) {
                            Ilk.Key key = new Ilk.Key(((ParameterizedType) stack.getLast().context.key.type).getActualTypeArguments()[0]);
                            stack.getLast().selected = injector.instance(key, null);
                        }
                    }
                }
                Ilk.Box context = getContext(stack);
                Type contextType = context == null ? null : ((ParameterizedType) context.key.type).getActualTypeArguments()[0];
                if (inStencilNamespace) {
                    stack.getLast().skip = true;
                    if (localName.equals("import")) {
                        if (!isText(nodes.get(index + 1))) {
                            throw new StencilException("Cannot load empty import at line [%s] of [%s].", element.line, uri);
                        }
                        try {
                            IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), (String) nodes.get(index + 1), Collections.<String, Class<?>>emptyMap());
                        } catch (ClassNotFoundException e) {
                            throw new StencilException(e,  "Cannot load type [%s] at line [%s] of [%s].", nodes.get(index + 1), element.line, uri);
                        }
                        page.imports.add(index);
                        index++;
                    } else if (localName.equals("include")) {
                        if (!isText(nodes.get(index + 1))) {
                            throw new StencilException("Cannot load empty include at line [%s] of [%s].", element.line, uri);
                        }
                        page.includes.add(index);
                        index++;
                    } else if (localName.equals("var")) {
                        String name = resolved.getValue(resolved.getIndex("name"));
                        if (isBlank(name)) {
                            throw new StencilException("Missing var name attribute at line [%s] of [%s].", element.line, uri);
                        }
                        String value = getString(contextType, name, getSelected(stack), element.line, uri);
                        if (output != null) {
                            stack.getLast().skip = true;
                            output.characters(value.toCharArray(), 0, value.length());
                        }
                    } else if (localName.equals("each")) {
                        String path = resolved.getValue(resolved.getIndex("path"));
                        if (isBlank(path)) {
                            throw new StencilException("Missing path attribute for each at line [%s] of [%s].", element.line, uri);
                        }
                        Ilk.Box value = get(contextType, path, getSelected(stack), element.line, uri);
                        if (!Collection.class.isAssignableFrom(getRawClass(value.key.type))) {
                            throw new StencilException("Missing path attribute for each at line [%s] of [%s].", element.line, uri);
                        }
                        Actualizer<T> actualizer = new Actualizer<T>(value.key.get(0).type);
                        if (output == null) {
                            stack.addLast(new Level());
                            stack.getLast().hasElement = true;
                            stack.getLast().context = actualizer.actual().box();
                            foo(injector, stack, uri, nodes, index + 1, page, output, lexical, dtd);
                            stack.removeLast();
                        } else {
                            for (T item : actualizer.collection(value)) {
                                stack.addLast(new Level());
                                stack.getLast().hasElement = true;
                                stack.getLast().context = actualizer.actual().box();
                                stack.getLast().selected = actualizer.actual().box(item);
                                foo(injector, stack, uri, nodes, index + 1, page, output, lexical, dtd);
                                stack.removeLast();
                            }
                        }
                        stack.getLast().skip = true;
                    } else if (localName.equals("if") || localName.equals("unless")) {
                        String path = resolved.getValue(resolved.getIndex("path"));
                        if (isBlank(path)) {
                            throw new StencilException("Missing path attribute for if at line [%d] of [%s]", element.line, uri);
                        }
                        // XXX Iterable needs to be converted into an iterator.
                        // XXX Identity hash map of iterators.
                        Ilk.Box value = get(contextType, path, getSelected(stack), element.line, uri);
                        boolean condition = false;
                        if (value != null) {
                            Class<?> rawClass = getRawClass(value.key.type);
                            if (rawClass.equals(Boolean.class)) {
                                condition = value.cast(new Ilk<Boolean>(Boolean.class));
                            } else if (Collection.class.isAssignableFrom(rawClass)) {
                                condition = ! ((Collection<?>) value.object).isEmpty();
                            } else if (Iterator.class.isAssignableFrom(rawClass)) {
                                condition = ((Iterator<?>) value.object).hasNext();
                            } else {
                                condition = true;
                            }
                        }
                        if (localName.equals("unless")) {
                            condition = !condition;
                        }
                        if (condition) {
                            stack.addLast(new Level());
                            stack.getLast().hasElement = true;
                            foo(injector, stack, uri, nodes, index + 1, page, output, lexical, dtd);
                            stack.removeLast();
                            Level level = stack.get(stack.size() - 2);
                            if (level.choose) {
                                level.skip = true;
                            }
                        }
                    } else if (localName.equals("default")) {
                        stack.addLast(new Level());
                        stack.getLast().hasElement = true;
                        foo(injector, stack, uri, nodes, index + 1, page, output, lexical, dtd);
                        stack.removeLast();
                        Level level = stack.get(stack.size() - 2);
                        if (level.choose) {
                            level.skip = true;
                        }
                    } else if (localName.equals("choose")) {
                        stack.getLast().choose = true;
                        stack.getLast().skip = false;
                    }
                }
                resolved = getAttributes(stack, resolved, contextType, getSelected(stack), element.line, uri);
                if (output != null && !stack.getLast().skip && !stack.getLast().choose) {
                    output.startElement(element.namespaceURI, element.localName, getQualifiedName(stack, element.namespaceURI, element.localName), resolved);
                }
            } else if (object instanceof String) {
                if (!stack.getLast().skip && !stack.getLast().choose) {
                    String characters = (String) object;
                    output.characters(characters.toCharArray(), 0, characters.length());
                }
            }
        }
 
    }

    // TODO Document.
    private Page compile(Injector injector, URI uri, List<Object> nodes, int offset, Page page, ContentHandler content, LexicalHandler lexical, DTDHandler dtd) throws SAXException {
        // Stack of state based on document element depth.
        LinkedList<Level> stack = new LinkedList<Level>();
        
        // Add a bogus top element to forgo empty stack tests.
        stack.addLast(new Level());
        stack.getLast().hasElement = true;
        
        if (content != null) {
            content.startDocument();
        }
        
        foo(injector, stack, uri, nodes, offset, page, content, lexical, dtd);
       
        if (content != null) {
            content.endDocument();
        }

        return page;
    }
    
    // TODO Document.
    private static String getQualifiedName(List<Level> stack, String namespaceURI, String localName) {
        if (namespaceURI.equals("")) {
            return localName;
        }
        ListIterator<Level> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level level = iterator.previous();
            String prefix = level.prefixes.get(namespaceURI);
            if (prefix != null) {
                return prefix.equals("") ? localName : prefix + ':' + localName;
            }
        }
        throw new StencilException("");
    }
    
    // TODO Document.
    private static boolean isText(Object object) {
        return object instanceof String;
    }
    
    // TODO Document.
    public static AttributesImpl getAttributes(LinkedList<Level> stack, AttributesImpl resolved, Type actual, Object object, int line, URI uri) {
        for (int j = 0, stop = resolved.getLength(); j < stop; j++) {
            if (resolved.getURI(j).equals(STENCIL_URI)) {
                String value = getString(actual, resolved.getLocalName(j), object, line, uri);
                if (object != null) {
                    String[] name = resolved.getValue(j).split(":");
                    if (name.length == 2) {
                        String namespaceURI = getNamespace(stack, name[0]);
                        resolved.setAttribute(j, namespaceURI, name[1], name[0] + ':' + name[1], "CDATA", diffuser.diffuse(value).toString());
                    } else if (name.length == 1) {
                        resolved.setAttribute(j, "", name[0], name[0], "CDATA", diffuser.diffuse(value).toString());
                    } else {
                        throw new StencilException("");
                    }
                }
            }
        }
        return resolved;
    }

    // TODO Document.
    private static String getString(Type actual, String expression, Object object, int line, URI uri) {
        Ilk.Box box = get(actual, expression, object, line, uri);
        return box == null ? null : diffuser.diffuse(box.object).toString();
    }
    
    // TODO Document.
    private static <T> Ilk.Box get(Type actual, String expression, Object object, int line, URI uri) {
        Path path;
        try {
            path = new Path(expression, false);
        } catch (ParseException e) {
            throw new StencilException(e, "Invalid object path [%s] at line [%d] of [%s].", expression, line, uri);
        }
        Type type = actual;
        for (int i = 0; i < path.size(); i++) {
            Part part = path.get(i);
            if (Map.class.isAssignableFrom(getRawClass(type))) {
                Map<?, ?> map = (Map<?, ?>) object;
                object = map.get(part.getName());
                type = ((ParameterizedType) type).getActualTypeArguments()[1];
            } else {
                Map<String, Getter> getters = Getters.getGetters(getRawClass(type));
                Getter getter = getters.get(part.getName());
                if (getter == null) {
                    throw new StencilException("Cannot evaluate path [%s] at line [%d] of [%s].", expression, line, uri);
                }
                if (object != null) {
                    try {
                        object = getter.get(object);
                    } catch (ReflectiveException e) {
                        throw new StencilException(e, "Cannot evaluate [%s] at line [%d] of [%s].", expression, line, uri);
                    }
                }
                type = getActualType(getter.getGenericType(), type);
            }
        }
        if (object == null) {
            return null;
        }
        return enbox(object, type);
    }
    
    // TODO Document.
    private static <T> Ilk.Box enbox(T object, Type type) {
        return new Ilk<T>(){}.assign(new Ilk<T>(){}, type).box(object);
    }
    
    // TODO Document.
    private static class Actualizer<T> {
        private Ilk<T> ilk;
        private final Type type;

        public Actualizer(Type type) {
            this.ilk = new Ilk<T>(){};
            this.type = type;
        }
        
        public Ilk<T> actual() {
            return ilk.assign(ilk, type);
        }
        
        public Collection<T> collection(Ilk.Box box) {
            return box.cast(new Ilk<Collection<T>>(){}.assign(ilk, type));
        }
    }
}
