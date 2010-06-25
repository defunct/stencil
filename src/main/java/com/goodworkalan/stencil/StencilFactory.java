package com.goodworkalan.stencil;

import static com.goodworkalan.ilk.Types.getActualType;
import static com.goodworkalan.ilk.Types.getRawClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

import com.goodworkalan.diffuse.Diffuser;
import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.permeate.ParseException;
import com.goodworkalan.permeate.Part;
import com.goodworkalan.permeate.Path;
import com.goodworkalan.reflective.ReflectiveException;
import com.goodworkalan.reflective.getter.Getter;
import com.goodworkalan.reflective.getter.Getters;

/**
 * Generates Stencil output using Stencils identified by URIs.
 *
 * @author Alan Gutierrez
 */
public class StencilFactory {
    /** The Stencil name space URI. */
    public final static String STENCIL_URI = "http://stencil.goodworkalan.com:2010/stencil";
    
    /** The namespace URI for attribute variables. */
    public final static String STENCIL_ATTRIBUTE_URI = "http://stencil.goodworkalan.com:2010/stencil/variable";
    
    /** The base URI from which resource URIs are resolved. */
    private URI baseURI;
    
    /** Whether to check if a URI resource is dirty and rebuild the stencil. */
    private boolean checkDirty = true;

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

    /**
     * Get the base URI used to resolve relative URIs.
     * 
     * @return The base URI.
     */
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
     * Get whether to check if a URI resource is dirty and rebuild the stencil.
     * 
     * @return True if stencils will be checked and recompiled if they have
     *         changed.
     */
    public synchronized boolean isCheckDirty() {
        return checkDirty;
    }

    /**
     * Set whether to check if a URI resource is dirty and rebuild the stencil.
     * 
     * @param checkDirty
     *            If true, stencils will be checked and recompiled if they have
     *            changed.
     */
    public synchronized void setCheckDirty(boolean checkDirty) {
        this.checkDirty = checkDirty;
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

    /**
     * Execute the stencil at the given URI using the given injector to obtain
     * context objects emitting a document to the given output writer.
     * 
     * @param injector
     *            The injector.
     * @param uri
     *            The URI.
     * @param output
     *            The writer output.
     */
    public void stencil(Injector injector, URI uri, Writer output) {
        URI resolved = baseURI.resolve(uri).normalize();
        Page page = stencils.get(resolved);
        if (page == null) {
            page = compile(injector, uri, output);
            stencils.put(resolved, page);
        } else if (checkDirty) {
            URL url;
            try {
                url = getURL(injector, resolved);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            URLConnection connection;
            try {
                connection = url.openConnection();
            } catch (IOException e) {
                throw new StencilException(e, "Cannot read [%s].", uri);
            }
            if (connection.getLastModified() > page.lastModified) {
                page = compile(injector, uri, connection, output);
                stencils.put(resolved, page);
            } else {
                compile(injector, uri, new Stencil(page, 0), output);
            }
        } else {
            compile(injector, uri, new Stencil(page, 0), output);
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

    /**
     * Determine if the given string is null or a zero length string and
     * therefore blank for our purposes.
     * 
     * @param string
     *            The string.
     * @return True if the string is null or a zero length string.
     */
    private boolean isBlank(String string) {
        return string == null || string.equals("");
    }

    /**
     * Resolve the URI into a URL that can be used to open an input stream to
     * obtain the resource. If a <code>ResourceResolver</code> has been
     * registered for the protocol of the URI, then the
     * <code>ResolverResolver</code> is used to obtain the URL. Otherwise, the
     * URI is converted to a URL using the {@link URI#toURL() URI.toURL} method.
     * 
     * @param injector
     *            The injector.
     * @param uri
     *            The URI to resolve.
     * @return A URL for the URI.
     * @throws MalformedURLException
     *             If the URI cannot be converted to a URL.
     */
    private URL getURL(Injector injector, URI uri) throws MalformedURLException {
        ResourceResolver resourceResolver = resourceResolvers.get(uri.getScheme());
        if (resourceResolver == null) {
            return uri.toURL();
        }
        return resourceResolver.getURL(injector, uri);
    }

    /**
     * Compile and execute the stencil indicated by the given URI returning a
     * stencil page for caching. The injector will be used to obtain context
     * objects for the stencils.
     * <p>
     * If the content handler is not null, the stencil will be emitted to the
     * given content, lexical and DTD handlers. The events for the lexical
     * handler will not be emitted if the lexical handler is null. The events
     * for the DTD handler will not be emitted if the DTD handler is null.
     * 
     * @param injector
     *            The injector.
     * @param uri
     *            The stencil URI.
     * @param content
     *            The SAX content handler or null if this is just a static
     *            check.
     * @param lexical
     *            The SAX lexical handler.
     * @param dtd
     *            The SAX DTD handler.
     * @return The compiled stencil page.
     */
    private Page compile(Injector injector, URI uri, Writer output) {
        // Normalize the absolute URI.
        uri = getBaseURI().resolve(uri).normalize();
        // Load the document.
        URLConnection connection;
        try {
            URL url = getURL(injector, uri);
            connection = url.openConnection();
        } catch (IOException e) {
            throw new StencilException(e, "Cannot read [%s].", uri);
        }
        return compile(injector, uri, connection, output);
    }

    /**
     * Compile and execute the stencil input stream of the given url connnection
     * obtained from the given url returning a stencil page for caching. The
     * injector will be used to obtain context objects for the stencils.
     * <p>
     * If the content handler is not null, the stencil will be emitted to the
     * given content, lexical and DTD handlers. The events for the lexical
     * handler will not be emitted if the lexical handler is null. The events
     * for the DTD handler will not be emitted if the DTD handler is null.
     * 
     * @param injector
     *            The injector.
     * @param uri
     *            The stencil URI.
     * @param content
     *            The SAX content handler or null if this is just a static
     *            check.
     * @param lexical
     *            The SAX lexical handler.
     * @param dtd
     *            The SAX DTD handler.
     * @return The compiled stencil page.
     */
    private Page compile(Injector injector, URI uri, URLConnection connection, Writer output) {
        List<String> lines;
        try {
            lines = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new StencilException("Cannot read URI resource [%s].", uri);
        }
        return compile(injector, uri, new Stencil(new Page(uri, connection.getLastModified(), lines), 0), output);
    }

    /**
     * Create a new in order traversal stack with a single empty root element so
     * that there is always a level and we don't have to keep checking for an
     * empty list.
     * 
     * @return An empty level stack.
     */
    private LinkedList<Level> newStack() {
        LinkedList<Level> stack = new LinkedList<Level>();
        stack.addLast(new Level());
        stack.getLast().hasElement = true;
        return stack;
    }
    
    private final static Pattern COMMAND = Pattern.compile("(.*)@(@|[A-Z][a-z]+)");

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
     * @param content
     *            The SAX content handler.
     * @param lexical
     *            The SAX lexical handler.
     * @param dtd
     *            The SAX DTD handler.
     * @return The index of the node after the last node processed.
     * @throws SAXException
     *             For any error raised by the parser.
     */
    private <T> int compile(Injector injector, LinkedList<Level> stack, LinkedList<Level> stencilStack, Stencil stencil, Stencil nested, Writer output)
    throws IOException {
        List<String> lines = stencil.page.lines;
//        int nestedIndex = nested.index;
//        boolean hasNested = false;
//        boolean hasUnnamedNested = false;
        int index = stencil.index;
        for (int stop = lines.size(), height = stack.size(); index < stop; index++) {
            String line = lines.get(index);
            Matcher command = COMMAND.matcher(line);
            while (command.matches()) {
                
            }
            if (output != null) {
                output.write(line);
                output.write("\n");
            }
        }
//            Object object = nodes.get(index);
//            if (object instanceof NotationDeclaration) {
//                if (dtd != null) {
//                    NotationDeclaration nd = (NotationDeclaration) object;
//                    dtd.notationDecl(nd.name, nd.publicId, nd.systemId);
//                }
//            } else if (object instanceof Comment) {
//                if (lexical != null) {
//                    String text = ((Comment) object).text;
//                    lexical.comment(text.toCharArray(), 0, text.length());
//                }
//            } else if (object instanceof DocumentTypeDefinition) {
//                if (lexical != null) {
//                    DocumentTypeDefinition doctype = (DocumentTypeDefinition) object;
//                    if (doctype.start) {
//                        lexical.startDTD(doctype.name, doctype.publicId, doctype.systemId);
//                    } else {
//                        lexical.endDTD();
//                    }
//                }
//            } else if (object instanceof String[]) {
//                if (!stack.getLast().skip) {
//                    String[] mapping = (String[]) object;
//                    if (stack.getLast().hasElement) {
//                        stack.addLast(new Level());
//                        stencilStack.addLast(new Level());
//                    }
//                    stencilStack.getLast().namespaceURIs.put(mapping[0], mapping[1]);
//                    stencilStack.getLast().prefixes.put(mapping[1], mapping[0]);
//                    stack.getLast().namespaceURIs.put(mapping[0], mapping[1]);
//                    stack.getLast().prefixes.put(mapping[1], mapping[0]);
//                    if (content != null) {
//                        content.startPrefixMapping(mapping[0], mapping[1]);
//                    }
//                }
//            } else if (object instanceof Element) {
//                Element element = (Element) object;
//                if (!element.start) {
//                    if (height == stack.size()) {
//                        break;
//                    }
//                    if (content != null) {
//                        if (!stack.getLast().skip && !stack.getLast().choose) {
//                            content.endElement(element.namespaceURI, element.localName, getQualifiedName(stack, element.namespaceURI, element.localName));
//                        }
//                        for (String prefix : stack.getLast().namespaceURIs.keySet()) {
//                            content.endPrefixMapping(prefix);
//                        }
//                    }
//                    stack.removeLast();
//                    stencilStack.removeLast();
//                    continue;
//                }
//                if (stack.getLast().hasElement) {
//                    boolean skip = stack.getLast().skip;
//                    stack.addLast(new Level());
//                    stencilStack.addLast(new Level());
//                    stack.getLast().skip = skip;
//                    stack.getLast().hasElement = skip;
//                }
//                if (stack.getLast().skip) {
//                    continue;
//                }
//                AttributesImpl resolved = new AttributesImpl(element.attributes);
//                stack.getLast().hasElement = true;
//                String localName = element.localName;
//                boolean inStencilNamespace = element.namespaceURI.equals(STENCIL_URI);
//                // Determine if we need to include any stencils.
//                {
//                    int level = stack.size() - 1;
//                    String include = null;
//                    if (inStencilNamespace && localName.equals("import")) {
//                        level--;
//                        stack.getLast().skip = true;
//                        include = resolved.getLocalName(resolved.getIndex("name"));
//                        if (isBlank(include)) {
//                            throw new StencilException("");
//                        }
//
//                    } else {
//                        int attribute = resolved.getIndex(STENCIL_URI, "import");
//                        if (attribute != -1) {
//                            include = resolved.getValue(attribute).trim();
//                            resolved.removeAttribute(attribute);
//                        }
//                    }
//                    if (!isBlank(include)) {
//                        Page included = compile(injector, stencil.page.uri.resolve(include), null, null, null);
//                        stack.get(level).stencils.putAll(included.stencils);
//                    }
//                }
//                {
//                    String name = null;
//                    int subIndex = index;
//                    if (inStencilNamespace && localName.equals("stencil")) {
//                        subIndex++;
//                        name = resolved.getLocalName(resolved.getIndex("name"));
//                        if (name == null) {
//                            throw new StencilException("");
//                        }
//                    } else  {
//                        int attribute = resolved.getIndex(STENCIL_URI, "stencil");
//                        if (attribute != -1) {
//                            name = resolved.getValue(attribute).trim();
//                            resolved.removeAttribute(attribute);
//                        }
//                    }
//                    if (!isBlank(name) && !stack.get(stack.size() - 2).isStencil) {
//                        stack.getLast().skip = true;
//                        String[] qualified = name.split(":");
//                        Stencil subStencil = new Stencil(stencil.page, subIndex);
//                        for (Level level : stack) {
//                            subStencil.namespaceURIs.putAll(level.namespaceURIs);
//                            subStencil.prefixes.putAll(level.prefixes);
//                        }
//                        stencil.page.stencils.put(new QName(getNamespace(stack, qualified[0]), qualified[1]), subStencil);
//                        LinkedList<Level> subStack = new LinkedList<Level>();
//                        subStack.addLast(new Level());
//                        subStack.getLast().hasElement = true;
//                        subStack.getLast().isStencil = true;
//                        index = compile(injector, subStack, newStack(), subStencil, new Stencil(), content, lexical, dtd) - 1;
//                        subStack.removeLast();
//                    }
//                }
//                { // Check for a new evaluation context.
//                    int attribute = resolved.getIndex(STENCIL_URI, "bind");
//                    if (attribute != -1) {
//                        String context = resolved.getValue(attribute);
//                        resolved.removeAttribute(attribute);
//                        if (!isBlank(context)) {
//                            try {
//                                stack.getLast().context = IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), context, Collections.<String, Class<?>>emptyMap());
//                            } catch (ClassNotFoundException e) {
//                                throw new StencilException(e, "Cannot load type [%s] at line [%s] of [%s].", context, element.line, stencil.page.uri);
//                            }
//                        }
//                        if (content != null) {
//                            Ilk.Key key = new Ilk.Key(((ParameterizedType) stack.getLast().context.key.type).getActualTypeArguments()[0]);
//                            stack.getLast().selected = injector.instance(key, null);
//                        }
//                    }
//                }
//                Ilk.Box context = getContext(stack);
//                Type contextType = context == null ? null : ((ParameterizedType) context.key.type).getActualTypeArguments()[0];
//                if (inStencilNamespace) {
//                    stack.getLast().skip = true;
//                    if (localName.equals("class")) {
//                        if (!(nodes.get(index + 1) instanceof String)) {
//                            throw new StencilException("Cannot load empty import at line [%s] of [%s].", element.line, stencil.page.uri);
//                        }
//                        Ilk.Box importation;
//                        try {
//                            importation = IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), (String) nodes.get(index + 1), Collections.<String, Class<?>>emptyMap());
//                        } catch (ClassNotFoundException e) {
//                            throw new StencilException(e,  "Cannot load type [%s] at line [%s] of [%s].", nodes.get(index + 1), element.line, stencil.page.uri);
//                        }
//                        stack.get(stack.size() - 2).imports.add(importation.key.get(0));
//                        index++;
//                    } else if (localName.equals("nested")) {
//                        String name = "";
//                        {
//                            int nameIndex = resolved.getIndex("name");
//                            if (nameIndex == -1) {
//                                if (hasNested) {
//                                    throw new StencilException("Only one nested element is allowed when unnamed nested elmeents are used at line [%s] of [%s].", element.line, stencil.page.uri);
//                                }
//                            } else {
//                                if (hasUnnamedNested) {
//                                    throw new StencilException("Cannot mix named and unnamed nested elements at line [%s] of [%s].", element.line, stencil.page.uri);
//                                }
//                                name = resolved.getValue(nameIndex).trim();
//                                if (isBlank(name)) {
//                                    throw new StencilException("Missing nested name attribute value at line [%s] of [%s].", element.line, stencil.page.uri);
//                                }
//                            }
//                        }
//                        if (nested.page.nodes.size() != nestedIndex) {
//                            if (name == "*") {
//                                Stencil subStencil = new Stencil(nested.page, nestedIndex);
//                                stack.addLast(new Level());
//                                stack.getLast().hasElement = true;
//                                compile(injector, stack, newStack(), subStencil, new Stencil(), content, lexical, dtd);
//                                stack.removeLast();
//                            } else {
//                                while (!(nested.page.nodes.get(nestedIndex) instanceof Element)) {
//                                    nestedIndex++;
//                                }
//                                if (nested.page.nodes.size() != nestedIndex) {
//                                    Element nestedElement = (Element) nested.page.nodes.get(nestedIndex);
//                                    if (nestedElement.start) {
//                                        String[] qualified = name.split(":");
//                                        qualified[0] = getNamespace(stencilStack, qualified[0]);
//                                        if (nestedElement.namespaceURI.equals(qualified[0]) && nestedElement.localName.equals(qualified[1])) {
//                                            nestedIndex++;
//                                            stack.addLast(new Level());
//                                            stack.getLast().hasElement = true;
//                                            nestedIndex = compile(injector, stack, newStack(), new Stencil(nested.page, nestedIndex), new Stencil(), content, lexical, dtd) + 1;
//                                            stack.removeLast();
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    } else if (localName.equals("var")) {
//                        String name = resolved.getValue(resolved.getIndex("path"));
//                        if (isBlank(name)) {
//                            throw new StencilException("Missing var name attribute at line [%s] of [%s].", element.line, stencil.page.uri);
//                        }
//                        String value = getString(contextType, name, getSelected(stack), element.line, stencil.page.uri);
//                        if (content != null) {
//                            stack.getLast().skip = true;
//                            content.characters(value.toCharArray(), 0, value.length());
//                        }
//                    } else if (localName.equals("each")) {
//                        String path = resolved.getValue(resolved.getIndex("path"));
//                        if (isBlank(path)) {
//                            throw new StencilException("Missing path attribute for each at line [%s] of [%s].", element.line, stencil.page.uri);
//                        }
//                        Ilk.Box value = get(contextType, path, getSelected(stack), element.line, stencil.page.uri);
//                        if (!Collection.class.isAssignableFrom(getRawClass(value.key.type))) {
//                            throw new StencilException("Missing path attribute for each at line [%s] of [%s].", element.line, stencil.page.uri);
//                        }
//                        Actualizer<T> actualizer = new Actualizer<T>(value.key.get(0).type);
//                        if (content == null) {
//                            stack.addLast(new Level());
//                            stack.getLast().hasElement = true;
//                            stack.getLast().context = actualizer.actual().box();
//                            compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), content, lexical, dtd);
//                            stack.removeLast();
//                        } else {
//                            for (T item : actualizer.collection(value)) {
//                                stack.addLast(new Level());
//                                stack.getLast().hasElement = true;
//                                stack.getLast().context = actualizer.actual().box();
//                                stack.getLast().selected = actualizer.actual().box(item);
//                                compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), content, lexical, dtd);
//                                stack.removeLast();
//                            }
//                        }
//                        stack.getLast().skip = true;
//                    } else if (localName.equals("if") || localName.equals("unless")) {
//                        String path = resolved.getValue(resolved.getIndex("path"));
//                        if (isBlank(path)) {
//                            throw new StencilException("Missing path attribute for if at line [%d] of [%s]", element.line, stencil.page.uri);
//                        }
//                        // XXX Iterable needs to be converted into an iterator.
//                        // XXX Identity hash map of iterators.
//                        Ilk.Box value = get(contextType, path, getSelected(stack), element.line, stencil.page.uri);
//                        boolean condition = false;
//                        if (value != null) {
//                            Class<?> rawClass = getRawClass(value.key.type);
//                            if (rawClass.equals(Boolean.class)) {
//                                condition = value.cast(new Ilk<Boolean>(Boolean.class));
//                            } else if (Collection.class.isAssignableFrom(rawClass)) {
//                                condition = ! ((Collection<?>) value.object).isEmpty();
//                            } else if (Iterator.class.isAssignableFrom(rawClass)) {
//                                condition = ((Iterator<?>) value.object).hasNext();
//                            } else {
//                                condition = true;
//                            }
//                        }
//                        if (localName.equals("unless")) {
//                            condition = !condition;
//                        }
//                        if (condition) {
//                            stack.addLast(new Level());
//                            stack.getLast().hasElement = true;
//                            compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), content, lexical, dtd);
//                            stack.removeLast();
//                            Level level = stack.get(stack.size() - 2);
//                            if (level.choose) {
//                                level.skip = true;
//                            }
//                        }
//                    } else if (localName.equals("default")) {
//                        stack.addLast(new Level());
//                        stack.getLast().hasElement = true;
//                        compile(injector, stack, newStack(), new Stencil(stencil.page, index + 1), new Stencil(), content, lexical, dtd);
//                        stack.removeLast();
//                        Level level = stack.get(stack.size() - 2);
//                        if (level.choose) {
//                            level.skip = true;
//                        }
//                    } else if (localName.equals("choose")) {
//                        stack.getLast().choose = true;
//                        stack.getLast().skip = false;
//                    }
//                }
//                resolved = getAttributes(stack, resolved, contextType, getSelected(stack), element.line, stencil.page.uri);
//                if (!isBlank(element.namespaceURI)) {
//                    Stencil subStencil = getStencil(stack, new QName(element.namespaceURI, element.localName));
//                    if (subStencil != null) {
//                        stack.getLast().skip = true;
//                        stack.addLast(new Level());
//                        stack.getLast().hasElement = true;
//                        stack.getLast().isStencil = true;
//                        LinkedList<Level> subStack = newStack();
//                        subStack.getLast().namespaceURIs.putAll(subStencil.namespaceURIs);
//                        subStack.getLast().prefixes.putAll(subStencil.prefixes);
//                        compile(injector, stack, subStack, subStencil, new Stencil(stencil.page, index + 1), content, lexical, dtd);
//                        stack.removeLast();
//                    }
//                }
//                if (content != null && !stack.getLast().skip && !stack.getLast().choose) {
//                    content.startElement(element.namespaceURI, element.localName, getQualifiedName(stack, element.namespaceURI, element.localName), resolved);
//                }
//            } else if (object instanceof String) {
//                if (content != null && !stack.getLast().skip && !stack.getLast().choose) {
//                    String characters = (String) object;
//                    content.characters(characters.toCharArray(), 0, characters.length());
//                }
//            }
//        }
        return index;
    }

    // TODO Document.
    private Page compile(Injector injector, URI uri, Stencil stencil, Writer output) {
        // Stack of state based on document element depth.
        LinkedList<Level> stack = new LinkedList<Level>();
        
        // Add a bogus top element to forgo empty stack tests.
        stack.addLast(new Level());
        stack.getLast().hasElement = true;
        
        try {
            compile(injector, stack, newStack(), stencil, new Stencil(), output);
        } catch (IOException e) {
            throw new StencilException(e, "Cannot emit stencil [%s].", uri);
        }
        
        return stencil.page;
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
        throw new StencilException("Cannot find qualified name for " + namespaceURI);
    }
    
//    // TODO Document.
//    private static AttributesImpl getAttributes(LinkedList<Level> stack, AttributesImpl resolved, Type actual, Object object, int line, URI uri) {
//        for (int j = 0, stop = resolved.getLength(); j < stop; j++) {
//            if (resolved.getURI(j).equals(STENCIL_ATTRIBUTE_URI)) {
//                String value = getString(actual, resolved.getLocalName(j), object, line, uri);
//                if (object != null) {
//                    String[] name = resolved.getValue(j).split(":");
//                    if (name.length == 2) {
//                        String namespaceURI = getNamespace(stack, name[0]);
//                        resolved.setAttribute(j, namespaceURI, name[1], name[0] + ':' + name[1], "CDATA", diffuser.diffuse(value).toString());
//                    } else if (name.length == 1) {
//                        resolved.setAttribute(j, "", name[0], name[0], "CDATA", diffuser.diffuse(value).toString());
//                    } else {
//                        throw new StencilException("");
//                    }
//                }
//            }
//        }
//        return resolved;
//    }

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
