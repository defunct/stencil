package com.goodworkalan.stencil;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.goodworkalan.diffuse.Diffuser;
import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.Types;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.loader.IlkLoader;
import com.goodworkalan.permeate.ParseException;
import com.goodworkalan.permeate.Part;
import com.goodworkalan.permeate.Path;
import com.goodworkalan.reflective.ReflectiveException;
import com.goodworkalan.reflective.getter.Getter;
import com.goodworkalan.reflective.getter.Getters;

public class StencilFactory {
    /** The Stencil name space URI. */
    public final static String STENCIL_URI = "http://stencil.goodworkalan:2010/stencil";

    private final static String XMLNS = "xmlns";
    
    /** The base URI from which resource URIs are resolved. */
    private URI baseURI;
    
    private Injector injector;
    
    private static final Diffuser diffuser = new Diffuser();
    
    /** The cache of compiled and verified stencils. */
    private final ConcurrentMap<URI, Page> stencils = new ConcurrentHashMap<URI, Page>();
    
    public synchronized URI getBaseURI() {
        return baseURI;
    }

    public synchronized void setBaseURI(URI uri) {
        this.baseURI = uri;
    }
    
    public void setInjector(Injector injector) {
        this.injector = injector;
    }
    
    protected InputStream getInputStream(URI uri) {
        try {
            return uri.toURL().openStream();
        } catch (IOException e) {
            throw new StencilException(e, "Unable to open stencil [%s].", uri);
        }
    }

    public void stencil(URI uri, ContentHandler handler) throws SAXException {
        URI resolved = baseURI.resolve(uri).normalize();
        Page page = stencils.get(resolved);
        if (page == null) {
            page = compile2(uri, handler);
            stencils.put(resolved, page);
        } else {
            compile(uri, page.nodes, 0, new Page(new ArrayList<Object>()), handler);
        }
    }
    
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
    
    private boolean isBlank(String string) {
        return string == null || string.equals("");
    }

    private Page compile2(URI uri, ContentHandler output) throws SAXException {
        // Normalize the absolute URI.
        uri = baseURI.resolve(uri).normalize();
        
        // Load the document.
        List<Object> nodes;
        try {
            nodes = InOrderDocument.readInOrderDocument(baseURI.resolve(uri).normalize().toURL().openStream());
        } catch (SAXException e) {
            throw new StencilException(e, "Invalid XML in [%s].", uri);
        } catch (IOException e) {
            throw new StencilException(e, "Cannot read [%s].", uri);
        }
        
        return compile(uri, nodes, 0, new Page(nodes), output);
    }

    private Page compile(URI uri, List<Object> nodes, int offset, Page page, ContentHandler output) throws SAXException {
        // Stack of state based on document element depth.
        LinkedList<Level> stack = new LinkedList<Level>();
        
        // Add a bogus top element to forgo empty stack tests.
        stack.addLast(new Level());
        stack.getLast().hasElement = true;
        
        if (output != null) {
            output.startDocument();
        }

        for (int index = offset, stop = nodes.size(); index < stop; index++) {
            Object object = nodes.get(index);
            if (object instanceof String[]) {
                String[] mapping = (String[]) object;
                if (stack.getLast().hasElement) {
                    stack.addLast(new Level());
                }
                stack.getLast().namespaceURIs.put(mapping[0], mapping[1]);
                stack.getLast().prefixes.put(mapping[1], mapping[0]);
                output.startPrefixMapping(mapping[0], mapping[1]);
            } else if (object instanceof Element) {
                Element element = (Element) object;
                if (!element.start) {
                    if (output != null && !stack.getLast().skip) {
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
                Type contextType = ((ParameterizedType) context.key.type).getActualTypeArguments()[0];
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
                        String value = get(contextType, name, getSelected(stack), element.line, uri);
                        if (output != null) {
                            stack.getLast().skip = true;
                            output.characters(value.toCharArray(), 0, value.length());
                        }
                    }
                }
                resolved = getAttributes(stack, resolved, contextType, getSelected(stack), element.line, uri);
                if (output != null && !stack.getLast().skip) {
                    output.startElement(element.namespaceURI, element.localName, getQualifiedName(stack, element.namespaceURI, element.localName), resolved);
                }
            } else if (object instanceof String) {
                if (!stack.getLast().skip) {
                    String characters = (String) object;
                    output.characters(characters.toCharArray(), 0, characters.length());
                }
            }
        }
        
        if (output != null) {
            output.endDocument();
        }

        return page;
    }
    
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
    
    private static boolean isText(Object object) {
        return object instanceof String;
    }
    
    public static AttributesImpl getAttributes(LinkedList<Level> stack, AttributesImpl resolved, Type actual, Object object, int line, URI uri) {
        for (int j = 0, stop = resolved.getLength(); j < stop; j++) {
            if (resolved.getURI(j).equals(STENCIL_URI)) {
                Object value = get(actual, resolved.getLocalName(j), object, line, uri);
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

    private static String get(Type actual, String expression, Object object, int line, URI uri) {
        Path path;
        try {
            path = new Path(expression, false);
        } catch (ParseException e) {
            throw new StencilException(e, "Invalid object path [%s] at line [%d] of [%s].", expression, line, uri);
        }
        Type type = actual;
        for (int i = 0; i < path.size(); i++) {
            Map<String, Getter> getters = Getters.getGetters(Types.getRawClass(type));
            Part part = path.get(i);
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
            type = Types.getActualType(getter.getType(), type);
        }
        return object == null ? null : diffuser.diffuse(object).toString();
    }
}
