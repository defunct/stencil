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
import java.util.Collections;
import java.util.Iterator;
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

    /**
     * The the value of the currently selected object.
     * 
     * @param stack
     *            The stack.
     * @return The value.
     */
    private <T> Object getSelected(LinkedList<Level<T>> stack) {
        ListIterator<Level<T>> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level<T> level = iterator.previous();
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
    private <T> Ilk.Box getContext(LinkedList<Level<T>> stack) {
        ListIterator<Level<T>> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level<T> level = iterator.previous();
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
    private <T> Stencil getStencil(LinkedList<Level<T>> stack, QName qName) {
        ListIterator<Level<T>> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level<T> level = iterator.previous();
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
    private <T> LinkedList<Level<T>> newStack() {
        LinkedList<Level<T>> stack = new LinkedList<Level<T>>();
        stack.addLast(new Level<T>());
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
    private <T> int compile(Injector injector, LinkedList<Level<T>> stack, LinkedList<Level<T>> stencilStack, Stencil stencil, Stencil nested, Writer output)
    throws IOException {
        List<String> lines = stencil.page.lines;
        int index = stencil.index;
        int stop = lines.size();
        List<String> blankLines = new ArrayList<String>();
        while (index < stop) {
            String line = lines.get(index++);
            int indent = indent(line);
            if (indent < stack.getLast().indent) {
                if (isWhitespace(line)) {
                    blankLines.add(line);
                    continue;
                } else if (stack.getLast().command.equals("If")) {
                    stack.removeLast();
                    if (!stack.getLast().skip) {
                        for (String blankLine : blankLines) {
                            println(output, blankLine);
                        }
                    }
                    blankLines.clear();
                }
            } else {
                if (!stack.getLast().skip) {
                    for (String blankLine : blankLines) {
                        println(output, blankLine);
                    }
                }
                blankLines.clear();
            }
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
                        throw new StencilException(e, "Cannot load type [%s] at line [%s] of [%s].", payload, index, stencil.page.uri);
                    }
                    if (output != null) {
                        Ilk.Key key = stack.getLast().ilk.key.get(0);
                        stack.getLast().instance = injector.instance(key, null);
                    }
                } else if (name.equals("Get")) {
                    if (isBlank(payload)) {
                        throw new StencilException("Missing Get path at line [%s] of [%s].", index, stencil.page.uri);
                    }
                    String value = getString(ilkType, payload, getSelected(stack), index, stencil.page.uri);
                    if (!stack.getLast().skip ) {
                        print(output, value);
                    }
                } else if (name.equals("If") || name.equals("Unless")) {
                    if (!isBlank(payload)) {
                        Ilk.Box value = get(ilkType, payload, getSelected(stack), index, stencil.page.uri);
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
                        int lastIndent = stack.getLast().indent;
                        stack.addLast(new Level<T>());
                        if (output != null) {
                            stack.getLast().skip = !condition;
                            stack.getLast().met = !condition;
                        }
                        stack.getLast().command = name;
                        if (terminal == null && indent > lastIndent) {
                            stack.getLast().indent = indent;
                        }
                    } else {
                        if (!name.equals(stack.getLast().command)) {
                            throw new IllegalStateException();
                        }
                        stack.removeLast();
                    }
                } else if (name.equals("Else")) {
                    if (!"If".equals(stack.getLast().command)) {
                        throw new IllegalStateException();
                    }
                    stack.getLast().skip = output != null && !stack.getLast().met;
                } else if (name.equals("Each")) {
                    if (isBlank(payload)) {
                        if (!"Each".equals(stack.getLast().command)) {
                            throw new StencilException("End Each encountered without Each at line [%s] of [%s].", index, stencil.page.uri);
                        }
                        if (output == null || !stack.getLast().each.hasNext()) {
                            stack.removeLast();
                        } else {
                            stack.getLast().instance = stack.getLast().actualizer.actual().box(stack.getLast().each.next());
                            index = stack.getLast().eachIndex;
                            after = stack.getLast().eachAfter;
                        }
                    } else {
                        Ilk.Box value = get(ilkType, payload, getSelected(stack), index, stencil.page.uri);
                        if (!Collection.class.isAssignableFrom(getRawClass(value.key.type))) {
                            throw new StencilException("Missing path attribute for each at line [%s] of [%s].", index, stencil.page.uri);
                        }
                        stack.addLast(new Level<T>());
                        stack.getLast().actualizer = new Actualizer<T>(value.key.get(0).type);
                        stack.getLast().command = name;
                        stack.getLast().ilk = stack.getLast().actualizer.actual().box();
                        if (output != null) {
                            stack.getLast().each = stack.getLast().actualizer.collection(value).iterator();
                            if (stack.getLast().each.hasNext()) {
                                stack.getLast().instance = stack.getLast().actualizer.actual().box(stack.getLast().each.next());
                                stack.getLast().eachIndex = index;
                                stack.getLast().eachAfter = after;
                            } else {
                                stack.getLast().skip = true;
                            }
                        }
                    }
                } else if (name.equals("Separator")) {
                    if (payload.equals("")) {
                        
                    } else {
                        if (stack.getLast().each.hasNext()) {
                            print(output, payload);
                        }
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
    private <T> Page compile(Injector injector, URI uri, Stencil stencil, Writer output) {
        // Stack of state based on document element depth.
        LinkedList<Level<T>> stack = new LinkedList<Level<T>>();
        
        // Add a bogus top element to forgo empty stack tests.
        stack.addLast(new Level<T>());
        
        try {
            compile(injector, stack, this.<T>newStack(), stencil, new Stencil(), output);
        } catch (IOException e) {
            throw new StencilException(e, "Cannot emit stencil [%s].", uri);
        }
        
        return stencil.page;
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
}
