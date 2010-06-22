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
    public final static String STENCIL_URI = "http://stencil.goodworkalan.com:2010/stencil";                                                   
    
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

    /**
     * Set the base URI used to resolve relative URIs.
     * 
     * @param uri The base URI.
     */
    public synchronized void setBaseURI(URI uri) {
        this.baseURI = uri;
    }

    /**
     * Open the input stream of the given url. This method exists to wrap the
     * <code>IOException</code> and to make <code>IOException</code> handling
     * testable.
     * 
     * @param uri
     *            The URI.
     * @return The opened input stream.
     * @exception StencilException
     *                If the stream cannot be opened.
     */
    InputStream getInputStream(URI uri) {
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
            page = compile(injector, uri, content, lexical, dtd);
            stencils.put(resolved, page);
        } else {
            compile(injector, uri, page.nodes, 0, page, content, lexical, dtd);
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

    /**
     * The the value of the currently selected object.
     * 
     * @param stack
     *            The stack.
     * @return The value.
     */
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

    /**
     * Get the boxed type of the context as indicated by a context attribute in
     * the stencil.
     * 
     * @param stack
     *            The stack.
     * @return The boxed type of the context object.
     */
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

    /**
     * Get the stencil with the given qualified name.
     * 
     * @param stack
     *            The stack.
     * @param qName
     *            The qualified name of the stencil.
     * @return The boxed type of the context object.
     */
    private Stencil getStencil(LinkedList<Level> stack, QName qName) {
        ListIterator<Level> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level level = iterator.previous();
            Stencil stencil = level.stencils.get(qName);
            if (stencil != null) {
                return stencil;
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
    private Page compile(Injector injector, URI uri, ContentHandler output, LexicalHandler lexical, DTDHandler dtd) throws SAXException {
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
        
        return compile(injector, uri, nodes, 0, new Page(uri, nodes), output, lexical, dtd);
    }
    
    public LinkedList<Level> newStack() {
        LinkedList<Level> stack = new LinkedList<Level>();
        stack.addLast(new Level());
        stack.getLast().hasElement = true;
        return stack;
    }

    /**
     * Compile and possibly emit a section of a Stencil.
     * 
     * @param <T>
     *            Type variable used for variable substitution.
     * @param injector
     *            The injector.
     * @param stack
     *            The stack.
     * @param stencil
     *            The stencil.
     * @param nested
     *            The nested content.
     * @param output
     *            The SAX content handler.
     * @param lexical
     *            The SAX lexical handler.
     * @param dtd
     *            The SAX DTD handler.
     * @return The index of the node after the last node processed.
     * @throws SAXException
     *             For any error raised by the parser.
     */
    private <T> int compile(Injector injector, LinkedList<Level> stack, LinkedList<Level> stencilStack, Stencil stencil, Stencil nested, ContentHandler output, LexicalHandler lexical, DTDHandler dtd) throws SAXException {
        List<Object> nodes = stencil.page.nodes;
        int nestedIndex = nested.index;
        boolean hasNested = false;
        boolean hasUnnamedNested = false;
        int index = stencil.index;
        for (int stop = nodes.size(), height = stack.size(); index < stop; index++) {
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
                if (lexical != null) {
                    DocumentTypeDefinition doctype = (DocumentTypeDefinition) object;
                    if (doctype.start) {
                        lexical.startDTD(doctype.name, doctype.publicId, doctype.systemId);
                    } else {
                        lexical.endDTD();
                    }
                }
            } else if (object instanceof String[]) {
                if (!stack.getLast().skip) {
                    String[] mapping = (String[]) object;
                    if (stack.getLast().hasElement) {
                        stack.addLast(new Level());
                        stencilStack.addLast(new Level());
                    }
                    stencilStack.getLast().namespaceURIs.put(mapping[0], mapping[1]);
                    stencilStack.getLast().prefixes.put(mapping[1], mapping[0]);
                    stack.getLast().namespaceURIs.put(mapping[0], mapping[1]);
                    stack.getLast().prefixes.put(mapping[1], mapping[0]);
                    if (output != null) {
                        output.startPrefixMapping(mapping[0], mapping[1]);
                    }
                }
            } else if (object instanceof Element) {
                Element element = (Element) object;
                if (!element.start) {
                    if (height == stack.size()) {
                        break;
                    }
                    if (output != null) {
                        if (!stack.getLast().skip && !stack.getLast().choose) {
                            output.endElement(element.namespaceURI, element.localName, getQualifiedName(stack, element.namespaceURI, element.localName));
                        }
                        for (String prefix : stack.getLast().namespaceURIs.keySet()) {
                            output.endPrefixMapping(prefix);
                        }
                    }
                    stack.removeLast();
                    stencilStack.removeLast();
                    continue;
                }
                if (stack.getLast().hasElement) {
                    boolean skip = stack.getLast().skip;
                    stack.addLast(new Level());
                    stencilStack.addLast(new Level());
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
                // Determine if we need to include any stencils.
                {
                    int level = stack.size() - 1;
                    String include = null;
                    if (inStencilNamespace && localName.equals("import")) {
                        level--;
                        stack.getLast().skip = true;
                        include = resolved.getLocalName(resolved.getIndex("name"));
                        if (isBlank(include)) {
                            throw new StencilException("");
                        }

                    } else {
                        int attribute = resolved.getIndex(STENCIL_URI, "import");
                        if (attribute != -1) {
                            include = resolved.getValue(attribute).trim();
                            resolved.removeAttribute(attribute);
                        }
                    }
                    if (!isBlank(include)) {
                        Page included = compile(injector, stencil.page.uri.resolve(include), null, null, null);
                        stack.get(level).stencils.putAll(included.stencils);
                    }
                }
                {
                    String name = null;
                    int subIndex = index;
                    if (inStencilNamespace && localName.equals("stencil")) {
                        subIndex++;
                        name = resolved.getLocalName(resolved.getIndex("name"));
                        if (name == null) {
                            throw new StencilException("");
                        }
                    } else  {
                        int attribute = resolved.getIndex(STENCIL_URI, "stencil");
                        if (attribute != -1) {
                            name = resolved.getValue(attribute).trim();
                            resolved.removeAttribute(attribute);
                        }
                    }
                    if (!isBlank(name) && !stack.get(stack.size() - 2).isStencil) {
                        stack.getLast().skip = true;
                        String[] qualified = name.split(":");
                        Stencil subStencil = new Stencil(stencil.page, subIndex);
                        for (Level level : stack) {
                            subStencil.namespaceURIs.putAll(level.namespaceURIs);
                            subStencil.prefixes.putAll(level.prefixes);
                        }
                        stencil.page.stencils.put(new QName(getNamespace(stack, qualified[0]), qualified[1]), subStencil);
                        LinkedList<Level> subStack = new LinkedList<Level>();
                        subStack.addLast(new Level());
                        subStack.getLast().hasElement = true;
                        subStack.getLast().isStencil = true;
                        index = compile(injector, subStack, newStack(), subStencil, new Stencil(), output, lexical, dtd) - 1;
                        subStack.removeLast();
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
                                throw new StencilException(e, "Cannot load type [%s] at line [%s] of [%s].", context, element.line, stencil.page.uri);
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
                    if (localName.equals("class")) {
                        if (!isText(nodes.get(index + 1))) {
                            throw new StencilException("Cannot load empty import at line [%s] of [%s].", element.line, stencil.page.uri);
                        }
                        Ilk.Box importation;
                        try {
                            importation = IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), (String) nodes.get(index + 1), Collections.<String, Class<?>>emptyMap());
                        } catch (ClassNotFoundException e) {
                            throw new StencilException(e,  "Cannot load type [%s] at line [%s] of [%s].", nodes.get(index + 1), element.line, stencil.page.uri);
                        }
                        stack.get(stack.size() - 2).imports.add(importation.key.get(0));
                        index++;
                    } else if (localName.equals("nested")) {
                        String name = "";
                        {
                            int nameIndex = resolved.getIndex("name");
                            if (nameIndex == -1) {
                                if (hasNested) {
                                    throw new StencilException("Only one nested element is allowed when unnamed nested elmeents are used at line [%s] of [%s].", element.line, stencil.page.uri);
                                }
                            } else {
                                if (hasUnnamedNested) {
                                    throw new StencilException("Cannot mix named and unnamed nested elements at line [%s] of [%s].", element.line, stencil.page.uri);
                                }
                                name = resolved.getValue(nameIndex).trim();
                                if (isBlank(name)) {
                                    throw new StencilException("Missing nested name attribute value at line [%s] of [%s].", element.line, stencil.page.uri);
                                }
                            }
                        }
                        if (nested.page.nodes.size() != nestedIndex) {
                            if (name == "*") {
                                Stencil subStencil = new Stencil(nested.page, nestedIndex);
                                stack.addLast(new Level());
                                stack.getLast().hasElement = true;
                                compile(injector, stack, newStack(), subStencil, new Stencil(), output, lexical, dtd);
                                stack.removeLast();
                            } else {
                                while (!(nested.page.nodes.get(nestedIndex) instanceof Element)) {
                                    nestedIndex++;
                                }
                                if (nested.page.nodes.size() != nestedIndex) {
                                    Element nestedElement = (Element) nested.page.nodes.get(nestedIndex);
                                    if (nestedElement.start) {
                                        String[] qualified = name.split(":");
                                        qualified[0] = getNamespace(stencilStack, qualified[0]);
                                        if (nestedElement.namespaceURI.equals(qualified[0]) && nestedElement.localName.equals(qualified[1])) {
                                            nestedIndex++;
                                            stack.addLast(new Level());
                                            stack.getLast().hasElement = true;
                                            nestedIndex = compile(injector, stack, newStack(), new Stencil(nested.page, nestedIndex), new Stencil(), output, lexical, dtd) + 1;
                                            stack.removeLast();
                                        }
                                    }
                                }
                            }
                        }
                    } else if (localName.equals("var")) {
                        String name = resolved.getValue(resolved.getIndex("path"));
                        if (isBlank(name)) {
                            throw new StencilException("Missing var name attribute at line [%s] of [%s].", element.line, stencil.page.uri);
                        }
                        String value = getString(contextType, name, getSelected(stack), element.line, stencil.page.uri);
                        if (output != null) {
                            stack.getLast().skip = true;
                            output.characters(value.toCharArray(), 0, value.length());
                        }
                    } else if (localName.equals("each")) {
                        String path = resolved.getValue(resolved.getIndex("path"));
                        if (isBlank(path)) {
                            throw new StencilException("Missing path attribute for each at line [%s] of [%s].", element.line, stencil.page.uri);
                        }
                        Ilk.Box value = get(contextType, path, getSelected(stack), element.line, stencil.page.uri);
                        if (!Collection.class.isAssignableFrom(getRawClass(value.key.type))) {
                            throw new StencilException("Missing path attribute for each at line [%s] of [%s].", element.line, stencil.page.uri);
                        }
                        Actualizer<T> actualizer = new Actualizer<T>(value.key.get(0).type);
                        if (output == null) {
                            stack.addLast(new Level());
                            stack.getLast().hasElement = true;
                            stack.getLast().context = actualizer.actual().box();
                            compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), output, lexical, dtd);
                            stack.removeLast();
                        } else {
                            for (T item : actualizer.collection(value)) {
                                stack.addLast(new Level());
                                stack.getLast().hasElement = true;
                                stack.getLast().context = actualizer.actual().box();
                                stack.getLast().selected = actualizer.actual().box(item);
                                compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), output, lexical, dtd);
                                stack.removeLast();
                            }
                        }
                        stack.getLast().skip = true;
                    } else if (localName.equals("if") || localName.equals("unless")) {
                        String path = resolved.getValue(resolved.getIndex("path"));
                        if (isBlank(path)) {
                            throw new StencilException("Missing path attribute for if at line [%d] of [%s]", element.line, stencil.page.uri);
                        }
                        // XXX Iterable needs to be converted into an iterator.
                        // XXX Identity hash map of iterators.
                        Ilk.Box value = get(contextType, path, getSelected(stack), element.line, stencil.page.uri);
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
                            compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), output, lexical, dtd);
                            stack.removeLast();
                            Level level = stack.get(stack.size() - 2);
                            if (level.choose) {
                                level.skip = true;
                            }
                        }
                    } else if (localName.equals("default")) {
                        stack.addLast(new Level());
                        stack.getLast().hasElement = true;
                        compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), output, lexical, dtd);
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
                resolved = getAttributes(stack, resolved, contextType, getSelected(stack), element.line, stencil.page.uri);
                if (!isBlank(element.namespaceURI)) {
                    Stencil subStencil = getStencil(stack, new QName(element.namespaceURI, element.localName));
                    if (subStencil != null) {
                        stack.getLast().skip = true;
                        stack.addLast(new Level());
                        stack.getLast().hasElement = true;
                        stack.getLast().isStencil = true;
                        LinkedList<Level> subStack = newStack();
                        subStack.getLast().namespaceURIs.putAll(subStencil.namespaceURIs);
                        subStack.getLast().prefixes.putAll(subStencil.prefixes);
                        index = compile(injector, stack, subStack, subStencil, new Stencil(stencil.page, index + 1), output, lexical, dtd) - 1;
                        stack.removeLast();
                    }
                }
                if (output != null && !stack.getLast().skip && !stack.getLast().choose) {
                    output.startElement(element.namespaceURI, element.localName, getQualifiedName(stack, element.namespaceURI, element.localName), resolved);
                }
            } else if (object instanceof String) {
                if (output != null && !stack.getLast().skip && !stack.getLast().choose) {
                    String characters = (String) object;
                    output.characters(characters.toCharArray(), 0, characters.length());
                }
            }
        }
        return index;
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
        
        compile(injector, stack, newStack(), new Stencil(page, offset), new Stencil(), content, lexical, dtd);
       
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
