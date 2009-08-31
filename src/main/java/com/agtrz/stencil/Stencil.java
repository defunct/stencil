/* Copyright Alan Gutierrez 2006 */
package com.agtrz.stencil;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.xerces.parsers.SAXParser;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import com.habitsoft.xhtml.dtds.XhtmlEntityResolver;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.ParsingException;
import nu.xom.Text;
import nu.xom.ValidityException;

public class Stencil
{
    public final static String NS_SNIPPITS = "flag://stencil.agtrz.com/snippits";

    private final static DirtSimpleEvaluator EVALUATOR = new DirtSimpleEvaluator();

    private static Object evalDirtSimplePath(Object context, Object root, String path, boolean single)
    {
        Object result = null;
        if (path.startsWith("."))
        {
            result = EVALUATOR.get(root, path.substring(1));
        }
        else
        {
            result = EVALUATOR.get(context, path);
        }
        if (single && result != null && result.getClass().isArray())
        {
            result = ((Object[]) result)[0];
        }
        return result;
    }

    // FIXME This can just be a URL.
    public interface Source
    {
        public InputStream getInputStream();

        public boolean getDirty();
    }

    public final static class InputStreamSource
    implements Source
    {
        private final InputStream inputStream;

        public InputStreamSource(InputStream inputStream)
        {
            this.inputStream = inputStream;
        }

        public InputStream getInputStream()
        {
            return inputStream;
        }

        public boolean getDirty()
        {
            return false;
        }
    }

    public final static class URLSource
    implements Source
    {
        private final URL url;

        private long createTime;

        public URLSource(URL url)
        {
            this.url = url;
        }

        public InputStream getInputStream()
        {
            this.createTime = System.currentTimeMillis();
            try
            {
                return url.openStream();
            }
            catch (IOException e)
            {
                throw new Danger("io.connect", e);
            }
        }

        public boolean getDirty()
        {
            long lastModified;
            try
            {
                URLConnection connection = url.openConnection();
                lastModified = connection.getLastModified();
                connection.getInputStream().close();
            }
            catch (IOException e)
            {
                throw new Danger("io.connect", e);
            }

            return lastModified > createTime;
        }
    }

    public final static class FileSource
    implements Source
    {
        private final File file;

        private long createTime;

        public FileSource(File file)
        {
            this.file = file;
        }

        public InputStream getInputStream()
        {
            this.createTime = System.currentTimeMillis();
            try
            {
                return new FileInputStream(file);
            }
            catch (FileNotFoundException e)
            {
                throw new Danger("io.connect", e);
            }
        }

        public boolean getDirty()
        {
            return file.lastModified() > createTime;
        }
    }

    public final static class Generator
    {
        private final List listOfTemplates;

        public Generator()
        {
            this.listOfTemplates = new ArrayList();
        }

        public void addTemplate(Template template)
        {
            listOfTemplates.add(0, template);
        }

        public Document bind(String name, Object context)
        {
            Iterator templates = listOfTemplates.iterator();
            while (templates.hasNext())
            {
                Template template = (Template) templates.next();
                Snippit snippit = template.newSnippit(name);
                if (snippit != null)
                {
                    snippit.bind(context, context, listOfTemplates);
                    return snippit.getDocument();
                }
            }
            return null;
        }
    }

    public final static class Template
    {
        private final Source source;

        private final Document xml;

        private final Map mapOfSnippits;

        public Template(InputStream in) throws ValidityException, ParsingException, IOException
        {
            this(new InputStreamSource(in));
        }

        public Template(Source source) throws ValidityException, ParsingException, IOException
        {
            SAXParser parser = new SAXParser();
            parser.setEntityResolver(new XhtmlEntityResolver());          
            Builder builder = new Builder(parser, false);
            this.source = source;
            this.xml = builder.build(source.getInputStream());
            this.mapOfSnippits = newMapOfSnippits(xml);
        }

        private final static Map newMapOfSnippits(Document xml)
        {
            Map mapOfSnippits = new LinkedHashMap();
            process(mapOfSnippits, xml.getRootElement());
            return mapOfSnippits;
        }

        private final static void process(Map mapOfSnippets, Element element)
        {
            Attribute name = null;
            if (element.getNamespaceURI().equals(NS_SNIPPITS) && element.getLocalName().equals("snippit"))
            {
                name = element.getAttribute("name");
            }
            else
            {
                name = element.getAttribute("snippit", NS_SNIPPITS);
            }
            if (name != null)
            {
                if (element.getParent() instanceof Document)
                {
                    mapOfSnippets.put(name.getValue(), element.getParent());
                }
                else
                {
                    mapOfSnippets.put(name.getValue(), new Document((Element) element.copy()));
                }
                name.detach();
            }
            Elements elements = element.getChildElements();
            for (int i = 0; i < elements.size(); i++)
            {
                process(mapOfSnippets, elements.get(i));
            }
        }

        public Snippit newSnippit(String name)
        {
            Document document = null;
            synchronized (this)
            {
                if (source.getDirty())
                {
                    Builder builder = new Builder();
                    Document xml;
                    try
                    {
                        xml = builder.build(source.getInputStream());
                    }
                    catch (Exception e)
                    {
                        throw new Danger("Cannot parse xml.", e);
                    }
                    mapOfSnippits.putAll(newMapOfSnippits(xml));
                }
                document = (Document) mapOfSnippits.get(name);
            }
            return document == null ? null : new Snippit((Document) document.copy());
        }
    }

    public interface Operation
    {
        public boolean operate(Object context, Object root, List listOfTemplates);

        public void cancel();
    }

    public final static class AttributeAssignment
    implements Operation
    {
        private final Element element;

        private final String name;

        private final String path;

        public AttributeAssignment(Element element, String name, String path)
        {
            this.element = element;
            this.name = name;
            this.path = path;
        }

        public boolean operate(Object context, Object root, List listOfTemplates)
        {
            Object value = evalDirtSimplePath(context, root, path, true);
            if (value != null)
            {
                element.addAttribute(new Attribute(name, value.toString()));
            }
            return true;
        }

        public void cancel()
        {
        }
    }

    public final static class NSAttributeAssignment
    implements Operation
    {
        private final Element element;

        private final String namespaceURI;

        private final String name;

        private final String path;

        public NSAttributeAssignment(Element element, String namespaceURI, String name, String path)
        {
            this.element = element;
            this.namespaceURI = namespaceURI;
            this.name = name;
            this.path = path;
        }

        public boolean operate(Object context, Object root, List listOfTemplates)
        {
            Object value = evalDirtSimplePath(context, root, path, true);
            if (value != null)
            {
                element.addAttribute(new Attribute(name, namespaceURI, value.toString()));
            }
            return true;
        }

        public void cancel()
        {
        }
    }

    public final static class NodeAssignment
    implements Operation
    {
        private final Element element;

        private final String path;

        public NodeAssignment(Element element, String path)
        {
            this.element = element;
            this.path = path;
        }

        public boolean operate(Object context, Object root, List listOfTemplates)
        {
            Object value = evalDirtSimplePath(context, root, path, true);
            if (value == null)
            {
                element.detach();
            }
            else
            {
                element.getParent().replaceChild(element, new Text(value.toString()));
            }
            return true;
        }

        public void cancel()
        {
            element.detach();
        }
    }

    public final static class Each
    implements Operation
    {
        private final Element element;

        private final String path;

        public Each(Element element, String path)
        {
            this.element = element;
            this.path = path;
        }

        public boolean operate(Object context, Object root, List listOfTemplates)
        {
            Iterator iterator = Collections.EMPTY_LIST.iterator();

            Object value = evalDirtSimplePath(context, root, path, false);
            if (value != null)
            {
                if (value instanceof Map)
                {
                    iterator = ((Map) value).values().iterator();
                }
                else if (value instanceof Collection)
                {
                    iterator = ((Collection) value).iterator();
                }
                else if (value instanceof Iterator)
                {
                    iterator = (Iterator) value;
                }
                else if (value.getClass().isArray())
                {
                    List list = new ArrayList();
                    Object[] array = (Object[]) value;
                    for (int i = 0; i < array.length; i++)
                    {
                        list.add(array[i]);
                    }
                    iterator = list.iterator();
                }
                else
                {
                    iterator = Collections.singleton(value).iterator();
                }
            }

            ParentNode parent = element.getParent();
            while (iterator.hasNext())
            {
                Element copy = (Element) element.copy();
                copy.setLocalName("ignore");
                Document document = new Document(copy);

                Snippit snippit = new Snippit(document);
                snippit.bind(iterator.next(), root, listOfTemplates);

                while (copy.getChildCount() != 0)
                {
                    Node node = copy.getChild(0);
                    node.detach();
                    parent.insertChild(node, parent.indexOf(element));
                }
            }

            element.detach();

            return true;
        }

        public void cancel()
        {
            element.detach();
        }
    }

    public interface Test
    {
        public boolean isTrue(Object context, Object root);
    }

    public final static class True
    implements Test
    {
        public boolean isTrue(Object context, Object root)
        {
            return true;
        }
    }

    public final static class Condition
    implements Test
    {
        private final boolean whenTrue;

        private final String path;

        public Condition(String path, boolean whenTrue)
        {
            this.whenTrue = whenTrue;
            this.path = path;
        }

        public boolean isTrue(Object context, Object root)
        {
            Object value = evalDirtSimplePath(context, root, path, true);
            boolean isTrue;
            if (value instanceof Boolean)
            {
                isTrue = ((Boolean) value).booleanValue();
            }
            else if (value instanceof Collection)
            {
                isTrue = ((Collection) value).size() != 0;
            }
            else if (value instanceof Iterator)
            {
                isTrue = ((Iterator) value).hasNext();
            }
            else
            {
                isTrue = (value != null);
            }
            return isTrue == whenTrue;
        }
    }

    public final static class Branch
    implements Operation
    {
        private final Element element;

        private final Test test;

        public Branch(Element element, Test test)
        {
            this.element = element;
            this.test = test;
        }

        public boolean operate(Object context, Object root, List listOfTemplates)
        {
            boolean operate = test.isTrue(context, root);
            if (operate)
            {
                ParentNode parent = element.getParent();
                int index = parent.indexOf(element);

                Element copy = (Element) element.copy();
                copy.setLocalName("ignore");
                Document document = new Document(copy);

                Snippit snippit = new Snippit(document);
                snippit.bind(context, root, listOfTemplates);

                for (int i = copy.getChildCount() - 1; i != -1; i--)
                {
                    Node node = copy.getChild(i);
                    node.detach();
                    parent.insertChild(node, index);
                }
            }
            element.detach();
            return operate;
        }

        public void cancel()
        {
            element.detach();
        }
    }

    public final static class Switch
    implements Operation
    {
        private final Element element;

        private final List listOfConditions;

        public Switch(Element element)
        {
            this.element = element;
            this.listOfConditions = new ArrayList();
        }

        public Switch()
        {
            this(null);
        }

        public void add(Operation condition)
        {
            listOfConditions.add(condition);
        }

        public boolean operate(Object context, Object root, List listOfTemplates)
        {
            Iterator conditions = listOfConditions.iterator();
            while (conditions.hasNext())
            {
                Operation condition = (Operation) conditions.next();
                if (condition.operate(context, root, listOfTemplates))
                {
                    break;
                }
            }
            while (conditions.hasNext())
            {
                Operation condition = (Operation) conditions.next();
                condition.cancel();
            }

            if (element != null)
            {
                ParentNode parent = element.getParent();
                while (element.getChildCount() != 0)
                {
                    Node node = element.getChild(0);
                    node.detach();
                    parent.insertChild(node, parent.indexOf(element));
                }
                element.detach();
            }

            return true;
        }

        public void cancel()
        {
            element.detach();
        }
    }

    public final static class Invoke
    implements Operation
    {
        private final Element element;

        private final String name;

        public Invoke(Element element, String name)
        {
            this.element = element;
            this.name = name;
        }

        public boolean operate(Object context, Object root, List listOfTemplates)
        {
            Iterator templates = listOfTemplates.iterator();
            while (templates.hasNext())
            {
                Template template = (Template) templates.next();
                Snippit snippit = template.newSnippit(name);
                if (snippit != null)
                {
                    snippit.bind(context, root, listOfTemplates);
                    Document document = snippit.getDocument();
                    Element body = document.getRootElement();

                    ParentNode parent = element.getParent();
                    int index = parent.indexOf(element);

                    if (element.getNamespaceURI().equals(NS_SNIPPITS))
                    {
                        for (int i = body.getChildCount() - 1; i != -1; i--)
                        {
                            Node node = body.getChild(i);
                            node.detach();
                            parent.insertChild(node, index);
                        }
                    }
                    else
                    {
                        parent.insertChild(body.copy(), index);
                    }

                    element.detach();

                    break;
                }
            }
            return true;
        }

        public void cancel()
        {
            element.detach();
        }
    }

    public final static class Snippit
    {
        private final Document document;

        private final List listOfOperations;

        public Snippit(Document document)
        {
            List listOfOperations = new ArrayList();
            process(document.getRootElement(), listOfOperations, null);
            this.listOfOperations = listOfOperations;
            this.document = document;
        }

        private final static void process(Element element, List listOfOperations, Switch choose)
        {
            boolean recurse = true;
            if (element.getNamespaceURI().equals(NS_SNIPPITS))
            {
                if (element.getLocalName().equals("var"))
                {
                    String path = element.getAttributeValue("name");
                    if (path == null)
                    {
                        throw new Danger("unnamed.var");
                    }
                    listOfOperations.add(new NodeAssignment(element, path));
                }
                else if (element.getLocalName().equals("each"))
                {
                    String path = element.getAttributeValue("name");
                    if (path == null)
                    {
                        throw new Danger("unnamed.each");
                    }
                    listOfOperations.add(new Each(element, path));
                    recurse = false;
                }
                else if (element.getLocalName().equals("if"))
                {
                    String path = element.getAttributeValue("test");
                    if (path == null)
                    {
                        throw new Danger("unnamed.if");
                    }
                    if (choose == null)
                    {
                        choose = new Switch();
                        listOfOperations.add(choose);
                    }
                    choose.add(new Branch(element, new Condition(path, true)));
                    recurse = false;
                }
                else if (element.getLocalName().equals("unless"))
                {
                    String path = element.getAttributeValue("test");
                    if (path == null)
                    {
                        throw new Danger("unnamed.unless");
                    }
                    if (choose == null)
                    {
                        choose = new Switch();
                        listOfOperations.add(choose);
                    }
                    choose.add(new Branch(element, new Condition(path, false)));
                    recurse = false;
                }
                else if (element.getLocalName().equals("invoke"))
                {
                    String name = element.getAttributeValue("snippit");
                    if (name == null)
                    {
                        throw new Danger("unnamed.invoke");
                    }
                    if (choose == null)
                    {
                        choose = new Switch();
                        listOfOperations.add(choose);
                    }
                    choose.add(new Invoke(element, name));
                    recurse = false;
                }
                else if (element.getLocalName().equals("switch"))
                {
                    choose = new Switch(element);
                    listOfOperations.add(choose);
                }
                else if (element.getLocalName().equals("default"))
                {
                    if (choose == null)
                    {
                        choose = new Switch();
                    }
                    choose.add(new Branch(element, new True()));
                    recurse = false;
                }
            }

            for (int i = 0; i < element.getAttributeCount(); i++)
            {
                Attribute attribute = element.getAttribute(i);
                if (attribute.getNamespaceURI().equals(NS_SNIPPITS))
                {
                    String path = attribute.getLocalName();
                    String value = attribute.getValue();
                    if (value.indexOf(':') != -1)
                    {
                        String[] values = value.split(":");
                        if (values.length != 2)
                        {
                            throw new Danger("bad.attribute.name");
                        }
                        String prefix = element.getNamespaceURI(values[0]);
                        listOfOperations.add(new NSAttributeAssignment(element, prefix, value, path));
                    }
                    else
                    {
                        listOfOperations.add(new AttributeAssignment(element, value, path));
                    }
                    attribute.detach();
                    i--;
                }
            }

            if (recurse)
            {
                Elements elements = element.getChildElements();
                for (int i = 0; i < elements.size(); i++)
                {
                    process(elements.get(i), listOfOperations, choose);
                }
            }
        }

        public Document getDocument()
        {
            return document;
        }

        private void bind(Object context, Object root, List listOfTemplates)
        {
            Iterator operations = listOfOperations.iterator();
            while (operations.hasNext())
            {
                Operation operation = (Operation) operations.next();
                operation.operate(context, root, listOfTemplates);
            }
        }

        public void bind(Object object)
        {
            bind(object, object, Collections.EMPTY_LIST);
        }
    }

    public interface Lookup
    {
        public Object get(Object object, String name);
    }

    public final static class BeanLookup
    implements Lookup
    {
        public final Map mapOfProperties;

        public BeanLookup(Object object)
        {
            Map mapOfProperties = new HashMap();
            BeanInfo beanInfo;
            try
            {
                beanInfo = Introspector.getBeanInfo(object.getClass());
            }
            catch (IntrospectionException e)
            {
                throw new Danger("introspection", e);
            }
            PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < props.length; i++)
            {
                mapOfProperties.put(props[i].getName(), props[i]);
            }
            this.mapOfProperties = mapOfProperties;
        }

        public Object get(Object object, String name)
        {
            Object value = null;
            PropertyDescriptor property = (PropertyDescriptor) mapOfProperties.get(name);
            if (property != null)
            {
                try
                {
                    value = property.getReadMethod().invoke(object, new Object[0]);
                }
                catch (Exception e)
                {
                    throw new Danger("bean.get", e);
                }
            }
            return value;
        }
    }

    public final static class MapLookup
    implements Lookup
    {
        public Object get(Object object, String name)
        {
            return ((Map) object).get(name);
        }
    }

    public interface Variable
    {
        public Object getVariable(Object object);
    }

    public final static class BeanVariable
    implements Variable
    {
        private final PropertyDescriptor property;

        public BeanVariable(PropertyDescriptor property)
        {
            this.property = property;
        }

        public Object getVariable(Object object)
        {
            Object value = null;
            try
            {
                value = property.getReadMethod().invoke(object, new Object[0]);
            }
            catch (IllegalArgumentException e)
            {
                throw new Danger("illegal.argument", e);
            }
            catch (IllegalAccessException e)
            {
                throw new Danger("illegal.access", e);
            }
            catch (InvocationTargetException e)
            {
                throw new Danger("invocation.target", e);
            }
            return value;
        }
    }

    public final static class Danger
    extends Error
    {
        private final static long serialVersionUID = 200704022L;

        public Danger(String message)
        {
            super(message);
        }

        public Danger(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */