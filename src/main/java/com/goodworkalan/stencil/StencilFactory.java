package com.goodworkalan.stencil;

import static com.goodworkalan.ilk.Types.getActualType;
import static com.goodworkalan.ilk.Types.getRawClass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

import com.goodworkalan.danger.Danger;
import com.goodworkalan.diffuse.Diffuser;
import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.IlkReflect;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.InjectorBuilder;
import com.goodworkalan.ilk.inject.Vendor;
import com.goodworkalan.ilk.inject.alias.AliasVendor;
import com.goodworkalan.ilk.loader.IlkLoader;
import com.goodworkalan.permeate.ParseException;
import com.goodworkalan.permeate.Part;
import com.goodworkalan.permeate.Path;
import com.goodworkalan.reflective.getter.Getter;
import com.goodworkalan.reflective.getter.Getters;
import com.goodworkalan.utility.Primitives;

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
    private boolean checkDirty;

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
                throw new Danger(e, StencilFactory.class, "cannotReadURL", uri);
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
    private <T> Ilk.Box getSelected(LinkedList<Level<T>> stack) {
        ListIterator<Level<T>> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level<T> level = iterator.previous();
            if (level.instance != null) {
                return level.instance;
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
     * Get a map of all the classes that have been imported into the current
     * scope.
     * 
     * @param <T>
     *            The actualizer type variable of the stack.
     * @param stack
     *            The stack.
     * @return A map of class names to type keys.
     */
    private <T> Map<String, Ilk.Key> getClasses(LinkedList<Level<T>> stack) {
        Map<String, Ilk.Key> classes = new HashMap<String, Ilk.Key>();
        for (Level<T> level : stack) {
            classes.putAll(level.classes);
        }
        return classes;
    }

    /**
     * Get the <code>Escaper</code> assigned to the top most level.
     * 
     * @param <T>
     *            The actualizer type variable of the stack.
     * @param stack
     *            The stack.
     * @return A map of class names to type keys.
     */
    private <T> Escaper getEscaper(LinkedList<Level<T>> stack, String name) {
        ListIterator<Level<T>> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level<T> level = iterator.previous();
            Escaper escaper = level.escapers.get(name);
            if (escaper != null) {
                return escaper;
            }
        }
        return null;
    }

    /**
     * Get the stencil with the given qualified name.
     * 
     * @param stack
     *            The stack.
     * @param name
     *            The qualified name of the stencil.
     * @return The boxed type of the context object.
     */
    private <T> Stencil getStencil(LinkedList<Level<T>> stack, String name) {
        ListIterator<Level<T>> iterator = stack.listIterator(stack.size());
        while (iterator.hasPrevious()) {
            Level<T> level = iterator.previous();
            Stencil stencil = level.stencils.get(name);
            if (stencil != null) {
                return stencil;
            }
        }
        return null;
    }

    /**
     * Get the top most injector on the stack.
     * 
     * @param stack
     *            The stack.
     * @return The top most injector.
     */
    private <T> Injector getInjector(LinkedList<Level<T>> stack) {
        ListIterator<Level<T>> iterator = stack.listIterator(stack.size());
        for (;;) {
            Level<T> level = iterator.previous();
            Injector injector = level.injector;
            if (injector != null) {
                return injector;
            }
        }
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
     * @param output
     *            The output.
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
            throw new Danger(e, StencilFactory.class, "cannotReadURL", uri);
        }
        return compile(injector, uri, connection, output);
    }

    /**
     * Compile and execute the stencil input stream of the given URL connection
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
     * @param connection
     *            A URL connection opened from a URL created from the URI.
     * @param output
     *            The output.
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
            throw new Danger(e, StencilFactory.class, "cannotReadURL", uri);
        }
        return compile(injector, uri, new Stencil(new Page(uri, connection.getLastModified(), lines), 0), output);
    }

    /** The regular expression to match a start or end command. */
    private final static Pattern COMMAND = Pattern.compile("^((?:[^@]|@@)*)@(@|[A-Z][a-zA-Z]+(?:\\.[A-Za-z][A-Za-z0-9]+)?)(?:!|\\(([^)]*)\\))?(.*)$");

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
    private void print(Writer output, CharSequence string) throws IOException {
        if (output != null) {
            output.append(string);
        }
    }
    
    private <V> Vendor<?> createAlias(Ilk.Box key, Class<? extends Annotation> qualifier, Ilk.Box fromKey, Class<? extends Annotation> fromQualifier, int line, URI uri) {
        Ilk.Box[] arguments = new Ilk.Box[] {
                key,
                new Ilk<Class<? extends Annotation>>() {}.box(qualifier),
                fromKey,
                new Ilk<Class<? extends Annotation>>() {}.box(fromQualifier)
        };
        Class<?>[] parameters = new Class<?>[] { Ilk.class, Class.class, Ilk.class, Class.class };
        Constructor<?> constructor = getVendorAliasConstructor(parameters);
        Ilk<AliasVendor<?>> vendorIlk = new Ilk<AliasVendor<?>>() {};
        try {
            return IlkReflect.newInstance(IlkReflect.REFLECTOR, vendorIlk.key, constructor, arguments).cast(vendorIlk);
        } catch (Exception e) {
            throw new Danger(e, StencilFactory.class, "cannotCreateAlias",  line, uri);
        }
    }

    /**
     * Get the constructor of <code>AliasVendor</code> that takes the given
     * parameters.
     * 
     * @param parameters
     *            The constructor parameters.
     * @return The constructor.
     */
    private Constructor<?> getVendorAliasConstructor(Class<?>[] parameters) {
        try {
            return AliasVendor.class.getConstructor(parameters);
        } catch (Exception e) {
            // Not thrown unless you've dismantled the JAR.
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the number of spaces by which the line is indented.
     * 
     * @param line
     *            The line.
     * @return The number of spaces indented.
     */
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
     * @param output
     *            The writer.
     * @return The index of the node after the last node processed.
     * @throws SAXException
     *             For any error raised by the parser.
     */
    private <T> Stencil[] compile(LinkedList<Level<T>> stack, Stencil stencil, Stencil nested, String blockName, Writer output)
    throws IOException {
        List<String> lines = stencil.page.lines;
        int index = stencil.index;
        int stop = lines.size();
        List<String> blankLines = new ArrayList<String>();
        String after = stencil.after;
        int count = stencil.count;
        int indent = stencil.indent;
        int blockCount = 0;
        Stencil current = null;
        LINES: while (after != null || index < stop) {
            // Bothers me that I can't find a way to put the initialization
            // block before the while and remove the test for null after,
            // because it bothers me to have initialization within the loop that
            // runs only on the first pass, but we're trying to resume the line
            // processing in the middle of a line, so some sort of
            // re-orientation after the jump will be necessary, which means that
            // this first pass initialization doesn't really happen on the first
            // pass through the list of lines, we're jumping, not within this
            // one method, but definitely jumping around in the list of lines,
            // so we may as well make it readable.
            String line; 
            if (after == null) {
                line = lines.get(index++);
                if (isComment(line)) {
                    continue;
                }
                indent = indent(line);
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
                    } else if (stack.getLast().command.equals("Each")) {
                        if (output == null || !stack.getLast().each.hasNext()) {
                            stack.removeLast();
                        } else {
                            stack.getLast().instance = stack.getLast().actualizer.actual().box(stack.getLast().each.next());
                            index = stack.getLast().index;
                            after = null; // stack.getLast().after;
                            continue;
                        }
                    }
                } else {
                    if (!stack.getLast().skip) {
                        for (String blankLine : blankLines) {
                            println(output, blankLine);
                        }
                    }
                    blankLines.clear();
                }
                after = line;
                count = 0;
            } else {
                line = after;
            }
            String terminal = null;
            for (;;) {
                current = new Stencil(stencil.page, after, index, count, indent);
                if (indent < stack.getLast().indent) {
                    if (blockName != null && blockName.equals(stack.getLast().command)) {
                        stack.removeLast();
                        return new Stencil[] { current, null };
                    }
                }
                Matcher command = COMMAND.matcher(after);
                if (!command.lookingAt()) {
                    if (count == 0) {
                        terminal = line;
                    }
                    break;
                }
                String before = command.group(1);
                after = command.group(4);
                if (terminal != null || !isWhitespace(before)) {
                    terminal = after;
                    if (!stack.getLast().skip) {
                        print(output, before);
                    }
                }
                String name = command.group(2).trim();
                String payload = command.group(3);
                Ilk.Box ilk = getContext(stack);
                Type ilkType = ilk == null ? null : ilk.key.get(0).type;
                count++;
                Stencil subStencil = getStencil(stack, name);
                if (name.equals("Bind")) {
                    String[] binding = payload.split("\\s*=>\\s*");
                    String[] from = binding[binding.length == 1 ? 0 : 1].split("/");
                    final Ilk.Box fromIlk;
                    final Class<? extends Annotation> fromQualifier;
                    try {
                        fromIlk = IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), from[0], Collections.<String, Class<?>>emptyMap());
                    } catch (ClassNotFoundException e) {
                        throw new Danger(e, StencilFactory.class, "cannotLoadType", payload, index, stencil.page.uri);
                    }
                    if (from.length == 2) {
                        try {
                            fromQualifier = Thread.currentThread().getContextClassLoader().loadClass(from[1]).asSubclass(Annotation.class);
                        } catch (ClassNotFoundException e) {
                            throw new Danger(e, StencilFactory.class, "cannotLoadQualifier", payload, index, stencil.page.uri);
                        }
                    } else {
                        fromQualifier = null;
                    }
                    stack.getLast().ilk = fromIlk;
                    if (output != null) {
                        InjectorBuilder module;
                        if (binding.length == 1) {
                            final Vendor<?> bound = createAlias(fromIlk, Bound.class, fromIlk, fromQualifier, index, stencil.page.uri);
                            module = new InjectorBuilder() {
                                protected void build() {
                                    vendor(bound);
                                }
                            };
                        } else {
                            String[] to;
                            Ilk.Box toIlk;
                            Class<? extends Annotation> toQualifier;
                            to = binding[0].split("/");
                            try {
                                toIlk = IlkLoader.fromString(Thread.currentThread().getContextClassLoader(), to[0], Collections.<String, Class<?>>emptyMap());
                            } catch (ClassNotFoundException e) {
                                throw new Danger(e, StencilFactory.class, "cannotLoadType", from[0], index, stencil.page.uri);
                            }
                            if (from.length == 2) {
                                try {
                                    toQualifier = Thread.currentThread().getContextClassLoader().loadClass(to[1]).asSubclass(Annotation.class);
                                } catch (ClassNotFoundException e) {
                                    throw new Danger(e, StencilFactory.class, "cannotLoadQualifier", from[1], index, stencil.page.uri);
                                }
                            } else {
                                toQualifier = null;
                            }
                            final Vendor<?> bound = createAlias(fromIlk, Bound.class, toIlk, toQualifier, index, stencil.page.uri);
                            final Vendor<?> alias = createAlias(toIlk, toQualifier, fromIlk, fromQualifier, index, stencil.page.uri);
                            module = new InjectorBuilder() {
                                public void build() {
                                    vendor(bound);
                                    vendor(alias);
                                }
                            };
                        }
                        Injector parent;
                        Injector topInjector = stack.getLast().injector;
                        if (topInjector != null) {
                            parent = topInjector.getParent();
                        } else {
                            parent = getInjector(stack);
                        }
                        InjectorBuilder childInjector = parent.newInjector();
                        childInjector.module(module);
                        childInjector.scope(BlockScoped.class);
                        stack.getLast().injector = childInjector.newInjector();
                        Ilk.Key key = stack.getLast().ilk.key.get(0);
                        stack.getLast().instance = getInjector(stack).instance(key, Bound.class);
                    }
                } else if (name.equals("Escape")) {
                    String[] escape = payload.split("\\s*=>\\s*");
                    String key = "*";
                    String escaper = null;
                    if (escape.length == 1) {
                        escaper = escape[0];
                    } else if (escape.length == 2) {
                        key = escape[0];
                        escaper = escape[1];
                    }
                    if (escaper == null) {
                        throw new Danger(StencilFactory.class, "missingEscapeProperties", index, stencil.page.uri);
                    }
                    URL url = null;
                    Class<?> escaperClass = null;
                    Ilk.Key classKey = getClasses(stack).get(escaper);
                    if (classKey != null) {
                        escaperClass = getRawClass(classKey.type);
                    }
                    if (escaperClass == null) {
                        url = Thread.currentThread().getContextClassLoader().getResource(key);
                        if (url == null) {
                            try {
                                Pattern identifier = Pattern.compile("[$_\\w][$_\\w\\d]+(?:\\.[$_\\w][$_\\w\\d]+)");
                                if (identifier.matcher(escaper).matches()) {
                                    escaperClass = Thread.currentThread().getContextClassLoader().loadClass(escaper);
                                }
                            } catch (ClassNotFoundException e) {
                            }
                            if (escaperClass == null) {
                                URI resource = getBaseURI().resolve(escaper);
                                if (resource != null) {
                                    url = getURL(getInjector(stack), resource);
                                }
                            }
                        }
                    }
                    if (url != null) {
                        Map<Integer, String> entities = new HashMap<Integer, String>();
                        BufferedReader entityLines = new BufferedReader(new InputStreamReader(url.openStream()));
                        String entityLine;
                        while ((entityLine = entityLines.readLine()) != null) {
                            String[] mapping = entityLine.split("\\s+");
                            entities.put(Integer.parseInt(mapping[0]), mapping[1]);
                        }
                        stack.getLast().escapers.put(key, new CharCodeEscaper(entities));
                    } else {
                        try {
                            stack.getLast().escapers.put(key, (Escaper) escaperClass.newInstance());
                        } catch (Exception e) {
                            throw new Danger(e, StencilFactory.class, "cannotCreateEscaper", escaperClass, index, stencil.page.uri);
                        }
                    }
                } else if (name.equals("Get")) {
                    if (terminal == null) {
                        if (!stack.getLast().skip) {
                            print(output, before);
                        }
                        terminal = after;
                    }
                    String[] pathway = payload.split("\\s*=>\\s*");
                    String path = null;
                    String escaperName = "*";
                    if (pathway.length == 1) {
                        path = pathway[0];
                    } else if (pathway.length == 2) {
                        escaperName = pathway[0];
                        path = pathway[1];
                    }
                    if (path == null) {
                        throw new Danger(StencilFactory.class, "missingGetPath", index, stencil.page.uri);
                    }
                    String value = getString(ilkType, path, getSelected(stack), index, stencil.page.uri);
                    if (!stack.getLast().skip) {
                        Escaper escaper = getEscaper(stack, escaperName);
                        if (escaper == null) {
                            throw new Danger(StencilFactory.class, "unknownEscaper", index, stencil.page.uri);
                        }
                        print(output, escaper.escape(value));
                    }
                } else if (name.equals("If") || name.equals("Unless")) {
                    if (!isBlank(payload)) {
                        Ilk.Box value = get(ilkType, payload, getSelected(stack), index, stencil.page.uri);
                        boolean condition = false;
                        if (value != null) {
                            Class<?> rawClass = getRawClass(value.key.type);
                            if (Primitives.box(rawClass).equals(Boolean.class)) {
                                condition = (Boolean) value.object;
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
                } else if (name.equals("ElseIf")) {     
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
                        if (output != null) {
                            stack.getLast().skip = !condition;
                            stack.getLast().met = !condition;
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
                    if (payload == null) {
                        if (!"Each".equals(stack.getLast().command)) {
                            throw new Danger(StencilFactory.class, "mismatchedEndEach", index, stencil.page.uri);
                        }
                        if (output == null || !stack.getLast().each.hasNext()) {
                            stack.removeLast();
                        } else {
                            stack.getLast().instance = stack.getLast().actualizer.actual().box(stack.getLast().each.next());
                            index = stack.getLast().index;
                            after = stack.getLast().after;
                        }
                    } else {
                        Ilk.Box value;
                        if (payload.equals("")) {
                            value = getSelected(stack);
                        } else {
                            value = get(ilkType, payload, getSelected(stack), index, stencil.page.uri);
                        }
                        if (!Collection.class.isAssignableFrom(getRawClass(value.key.type))) {
                            throw new Danger(StencilFactory.class, "missingEachPath", index, stencil.page.uri);
                        }
                        int lastIndent = stack.getLast().indent;
                        stack.addLast(new Level<T>());
                        stack.getLast().actualizer = new Actualizer<T>(value.key.get(0).type);
                        stack.getLast().command = name;
                        stack.getLast().ilk = new Ilk.Box(stack.getLast().actualizer.actual().key.type);
                        if (output != null) {
                            stack.getLast().each = stack.getLast().actualizer.collection(value).iterator();
                            if (stack.getLast().each.hasNext()) {
                                stack.getLast().instance = stack.getLast().actualizer.actual().box(stack.getLast().each.next());
                                stack.getLast().index = index;
                                stack.getLast().after = after;
                            } else {
                                stack.getLast().skip = true;
                            }
                            if (terminal == null && indent > lastIndent) {
                                stack.getLast().indent = indent;
                            } else {
                                terminal = after;
                            }
                        }
                    }
                } else if (name.equals("Separator")) {
                    if (payload == null) {
                        if (!stack.getLast().each.hasNext()) {
                            stack.getLast().skip = true;
                        }
                    } else {
                        if (stack.getLast().each.hasNext()) {
                            print(output, payload);
                        }
                    }
                } else if (name.equals("Import")) {
                    String[] importation = payload.split("\\s*=>\\s*");
                    URI uri;
                    try {
                        uri = new URI(importation[1]);
                    } catch (URISyntaxException e) {
                        throw new Danger(e, StencilFactory.class, "malformedImportURI", index, stencil.page.uri);
                    }
                    uri = stencil.page.uri.resolve(uri);
                    String alias = importation[0];
                    for (Map.Entry<String, Stencil> entry : compile(getInjector(stack), uri, null).stencils.entrySet()) {
                        stack.getLast().stencils.put(alias + "." + entry.getKey(), entry.getValue());
                    }
                } else if (name.equals("Stencil")) {
                    if (payload == null) {
                        stack.removeLast();
                    } else {
                        stack.addLast(new Level<T>());
                        stack.getLast().command = name;
                        stencil.page.stencils.put(payload, current);
                    }
                } else if (name.equals("Nested")) {
                    if (nested != null) {
                        nested = compile(stack, nested, null, payload, output)[0];
                    }
                } else if (subStencil != null) {
//                    if (name.equals(stack.getLast().command) && bracket.equals("!")) {
//                        stack.removeLast();
//                    } else { 
                        if (subStencil == null) {
                            if (payload == null) {
                                throw new Danger(StencilFactory.class, "missingStencil", name, index, stencil.page.uri);
                            }
                            return new Stencil[] { current, null };
                        }
//                        int lastIndent = stack.getLast().indent;
//                        stack.addLast(new Level<T>());
//                        stack.getLast().command = name;
//                        if (indent > lastIndent) {
//                            stack.getLast().indent = indent;
//                        }
                        Stencil result = compile(stack, subStencil, new Stencil(stencil.page, after, index, count, indent), null, output)[1];
                        after = result.after;
                        count = result.count;
                        index = result.index;
                        continue LINES;
//                    }
                } else if (blockName != null) {
                    if (blockCount == 0) {
                        if (!blockName.equals(name)) {
                            throw new Danger(StencilFactory.class, "missingNestedBlock", blockName, index, stencil.page.uri);
                        }
                        blockCount++;
                        if (payload != null) {
                            print(output, payload);
                            return new Stencil[] { new Stencil(stencil.page, after, index, count, indent), null };
                        }
                        int lastIndent = stack.getLast().indent;
                        stack.addLast(new Level<T>());
                        stack.getLast().command = name;
                        if (terminal == null && indent > lastIndent) {
                            stack.getLast().indent = indent;
                        } else {
                            terminal = after;
                        }
                    } else if (!blockName.equals(name)){
                        stack.removeLast();
                        return new Stencil[] { current, null };
                    }
                }
                if (after.trim().length() == 0) {
                    break;
                }
            }
            if (terminal != null && !stack.getLast().skip) {
                println(output, terminal);
            }
            after = null;
        }
        return new Stencil[] { new Stencil(stencil.page, after, index, count, indent), nested };
    }

    /**
     * Determine if the given line is a comment line.
     * 
     * @param line
     *            The line.
     * @return True if the line is a comment.
     */
    private boolean isComment(String line) {
        int index = 0, stop = line.length();
        while (index < stop) {
            if (!Character.isWhitespace(line.charAt(index))) {
                break;
            }
            index++;
        }
        if (stop != index) {
            if (line.charAt(index) == '@') {
                index++;
                if (stop != index) {
                    return Character.isWhitespace(line.charAt(index));
                }
            }
        }
        return false;
    }

    /**
     * Determine if the given line is entirely whitespace.
     * 
     * @param line
     *            The line.
     * @return True if the line is entirely whitespace.
     */
    private boolean isWhitespace(String line) {
        for (int i = 0, stop = line.length(); i < stop; i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compile the given stencil located at the given URI. If the output writer
     * is null, the compilation is a static analysis, all branches will be
     * checked for type safety.
     * 
     * @param <T>
     *            Type variable used for variable substitution.
     * @param injector
     *            The injector.
     * @param uri
     *            The uri of the stencil.
     * @param stencil
     *            The stencil.
     * @param output
     *            The output writer or null if this is a static analysis.
     * @return A page containing the lines and stencils found in the stencil.
     */
    private <T> Page compile(Injector injector, URI uri, Stencil stencil, Writer output) {
        // Stack of state based on document element depth.
        LinkedList<Level<T>> stack = new LinkedList<Level<T>>();
        
        // Add a bogus top element to forgo empty stack tests.
        stack.addLast(new Level<T>());
        stack.getLast().escapers.put("*", new Escaper());
        stack.getLast().injector = injector.newInjector().newInjector(); 
        
        try {
            compile(stack, stencil, null, null, output);
        } catch (IOException e) {
            throw new Danger(e, StencilFactory.class, "ioException", uri);
        }
        
        return stencil.page;
    }

    /**
     * Get the string value of the object found at the given path, checking that
     * the path actually exists relative to the given object.
     * 
     * @param actual
     *            The type of the object.
     * @param expression
     *            The path to evaluate.
     * @param object
     *            The object.
     * @param line
     *            The line number where the path was read.
     * @param uri
     *            The URI from which the path was read.
     * @return The string value of the given object or null.
     */
    private static String getString(Type actual, String expression, Ilk.Box object, int line, URI uri) {
        Ilk.Box box = get(actual, expression, object, line, uri);
        return box == null ? null : diffuser.diffuse(box.object).toString();
    }

    /**
     * Get a boxed instance of the object found at the given path, checking that
     * the path actually exists relative to the given object.
     * 
     * @param actual
     *            The type of the object.
     * @param expression
     *            The path to evaluate.
     * @param object
     *            The object.
     * @param line
     *            The line number where the path was read.
     * @param uri
     *            The URI from which the path was read.
     * @return A boxed instance of the object.
     */
    private static <T> Ilk.Box get(Type actual, String expression, Ilk.Box object, int line, URI uri) {
        Path path;
        try {
            path = new Path(expression, false);
        } catch (ParseException e) {
            throw new Danger(e, StencilFactory.class, "Invalid object path [%s] at line [%d] of [%s].", expression, line, uri);
        }
        Type type = object.key.type;
        for (int i = 0; i < path.size(); i++) {
            Part part = path.get(i);
            if (PathEvaluator.class.isAssignableFrom(getRawClass(type))) {
                if (object != null) {
                    try {
                        Method method = getRawClass(type).getMethod("get", Ilk.Box.class, Path.class);
                        object = IlkReflect.invoke(IlkReflect.REFLECTOR, method, object, new Ilk<Ilk.Box>(Ilk.Box.class).box(object), new Ilk<Path>(Path.class).box(path));
                    } catch (Throwable e) {
                        throw new Danger(e, StencilFactory.class, "Cannot invoke get on PathEvaluator at line [%d] of [%s].", line, uri);
                    }
                }
                type = ((ParameterizedType) type).getActualTypeArguments()[0];
                break;
            } else if (Map.class.isAssignableFrom(getRawClass(type))) {
                if (object != null) {
                    try {
                        Method method = getRawClass(type).getMethod("get", Object.class);
                        object = IlkReflect.invoke(IlkReflect.REFLECTOR, method, object, new Ilk<String>(String.class).box(part.getName()));
                    } catch (Exception e) {
                        throw new Danger(e, StencilFactory.class, "cannotInvokeGetOnMap", line, uri);
                    }
                }
                type = ((ParameterizedType) type).getActualTypeArguments()[1];
            } else {
                Map<String, Getter> getters = Getters.getGetters(getRawClass(type));
                Getter getter = getters.get(part.getName());
                if (getter == null) {
                    throw new Danger(StencilFactory.class, "invalidObjectPath", expression, line, uri);
                }
                if (object != null) {
                    try {
                        if (getter.getMember() instanceof Method) {
                            Method method = (Method) getter.getMember();
                                object = IlkReflect.invoke(IlkReflect.REFLECTOR, method, object);
                        } else {
                            Field field = (Field) getter.getMember();
                            object = IlkReflect.get(IlkReflect.REFLECTOR, field, object);
                        }
                    } catch (Exception e) {
                        throw new Danger(e, StencilFactory.class, "cannotInvokeGet", part.getName(), object.getClass(), line, uri);
                    }
                }
                type = getActualType(getter.getGenericType(), type, new LinkedList<Map<TypeVariable<?>, Type>>());
            }
        }
        return object;
    }
}
