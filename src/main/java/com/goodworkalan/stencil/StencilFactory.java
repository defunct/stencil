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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.xml.sax.SAXException;

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
            if (level.instance != null) {
                return level.instance.object;
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
            if (level.ilk != null) {
                return level.ilk;
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
    
    private final static Pattern COMMAND = Pattern.compile("^((?:[^@]|@@)*)@(@|[A-Z][a-z]+)(?:\\s*\\(([^)]*)\\))?(.*)$");

    /**
     * Write the given string to the the given writer followed by a new line if
     * the writer is not null.
     * 
     * @param output
     *            The output.
     * @param string
     *            The string.
     * @throws IOException
     *             For any I/O error.
     */
    private void println(Writer output, String string) throws IOException {
        if (output != null) {
            output.write(string);
            output.write("\n");
        }
    }

    /**
     * Write the given string to the the given writer if the writer is not null.
     * 
     * @param output
     *            The output.
     * @param string
     *            The string.
     * @throws IOException
     *             For any I/O error.
     */
    private void print(Writer output, String string) throws IOException {
        if (output != null) {
            output.write(string);
        }
    }

    private final static Set<String> INLINE_COMMANDS = new HashSet<String>(Arrays.asList(
            "If", "Elsif", "Get", "Each"
    ));

    private final static Set<String> BLOCK_COMMANDS = new HashSet<String>(Arrays.asList(
            "Bind"
    ));
    
    private int indent(String line) {
        int count = 0, stop = line.length();
        while (count < stop && Character.isWhitespace(line.charAt(count))) {
            count++;
        }
        return count;
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
            boolean inline = true;
            int indent = 0;
            String line = lines.get(index);
            String after = line;
            int count = 0;
            String terminal = null;
            for (;;) {
                Matcher command = COMMAND.matcher(after);
                if (!command.lookingAt()) {
                    if (count == 0) {
                        terminal = line;
                    }
                    break;
                }
                String before = command.group(1);
                if (count == 0) {
                    indent = indent(before);
                }
                after = command.group(4);
                if (count != 0 || !isWhitespace(before)) {
                    terminal = after;
                    if (!stack.getLast().skip) {
                        print(output, before);
                    }
                }
                String name = command.group(2).trim();
                String payload = command.group(3);
                Ilk.Box ilk = getContext(stack);
                Type ilkType = ilk == null ? null : ilk.key.get(0).type;
                if (name.equals("Bind")) {
                    try {
                        stack.getLast().ilk = IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), payload, Collections.<String, Class<?>>emptyMap());
                    } catch (ClassNotFoundException e) {
                        throw new StencilException(e, "Cannot load type [%s] at line [%s] of [%s].", payload, index + 1, stencil.page.uri);
                    }
                    if (output != null) {
                        Ilk.Key key = stack.getLast().ilk.key.get(0);
                        stack.getLast().instance = injector.instance(key, null);
                    }
                } else if (name.equals("Get")) {
                    if (isBlank(payload)) {
                        throw new StencilException("Missing Get path at line [%s] of [%s].", index + 1, stencil.page.uri);
                    }
                    String value = getString(ilkType, payload, getSelected(stack), index + 1, stencil.page.uri);
                    if (!stack.getLast().skip ) {
                        print(output, value);
                    }
                } else if (name.equals("If") || name.equals("Unless")) {
                    if (!isBlank(payload)) {
                        Ilk.Box value = get(ilkType, payload, getSelected(stack), index + 1, stencil.page.uri);
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
                        if (name.equals("Unless")) {
                            condition = !condition;
                        }
                        stack.addLast(new Level());
                        stack.getLast().skip = !condition;
                        stack.getLast().command = name;
                    } else {
                        if (!name.equals(stack.getLast().command)) {
                            throw new IllegalStateException();
                        }
                        stack.removeLast();
                    }
                }
                if (after.trim().length() == 0) {
                    break;
                }
                count++;
            }
            if (terminal != null && !stack.getLast().skip) {
                println(output, terminal);
            }
        }
        return index;
    }

    public boolean isWhitespace(String string) {
        for (int i = 0, stop = string.length(); i < stop; i++) {
            if (!Character.isWhitespace(string.charAt(i))) {
                return false;
            }
        }
        return true;
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
