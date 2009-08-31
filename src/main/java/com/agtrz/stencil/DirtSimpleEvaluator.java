/* Copyright Alan Gutierrez 2006 */
package com.agtrz.stencil;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DirtSimpleEvaluator
{
    private final Map mapOfCreators;

    private final Converter converter;

    public final static Converter NULL_CONVERTER = new NullConverter();

    public DirtSimpleEvaluator()
    {
        this.mapOfCreators = new HashMap();
        this.converter = new ServletConverter();
    }

    public interface Creator
    {
        public Object newObject(String key);
    }

    public void addCreator(String path, Creator creator)
    {
        mapOfCreators.put(path, creator);
    }

    public void set(Object object, String path, Object value)
    {
        Object result = object;
        String[] segments = path.split("\\.");
        for (int i = 0; result != null && i < segments.length - 1; i++)
        {
            if (segments[i].equals("this"))
            {
                continue;
            }
            Lookup lookup = (result instanceof Map) ? (Lookup) new MapLookup() : (Lookup) new BeanLookup(result);
            Object next = lookup.get(result, segments[i]);
            if (next == null)
            {
                String parent = "";
                String separator = "";
                for (int j = 0; j < i; j++)
                {
                    parent += separator + segments[j];
                    separator = ".";
                }
                Creator creator = (Creator) mapOfCreators.get(parent);
                if (creator != null)
                {
                    next = creator.newObject(segments[i]);
                    lookup.put(NULL_CONVERTER, result, segments[i], next);
                }
            }
            result = next;
        }
        if (result != null)
        {
            Lookup lookup = (result instanceof Map) ? (Lookup) new MapLookup() : (Lookup) new BeanLookup(result);
            lookup.put(converter, result, segments[segments.length - 1], value);
        }
    }

    public Object get(Object object, String path)
    {
        Object result = object;
        String[] segments = path.split("\\.");
        for (int i = 0; result != null && i < segments.length; i++)
        {
            if (segments[i].equals("this"))
            {
                continue;
            }
            Lookup lookup = (result instanceof Map) ? (Lookup) new MapLookup() : (Lookup) new BeanLookup(result);
            result = lookup.get(result, segments[i]);
        }
        return result;
    }

    public interface Lookup
    {
        public Object get(Object object, String name);

        public void put(Converter converter, Object object, String name, Object value);
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

        public void put(Converter converter, Object object, String name, Object value)
        {
            PropertyDescriptor property = (PropertyDescriptor) mapOfProperties.get(name);
            if (property != null)
            {
                Method writeMethod = property.getWriteMethod();
                if (writeMethod != null)
                {
                    Object converted = converter.convert(writeMethod.getParameterTypes()[0], value);
                    try
                    {
                        writeMethod.invoke(object, new Object[] { converted });
                    }
                    catch (Exception e)
                    {
                        throw new Danger("bean.set", e);
                    }
                }
            }
        }
    }

    public final static class MapLookup
    implements Lookup
    {
        public Object get(Object object, String name)
        {
            return ((Map) object).get(name);
        }

        public void put(Converter converter, Object object, String name, Object value)
        {
            ((Map) object).put(name, converter.convert(Object.class, value));
        }
    }

    public interface Converter
    {
        public Object convert(Class propertyType, Object object);
    }

    public final static class ServletConverter
    implements Converter
    {
        public Object convert(Class propertyType, Object object)
        {
            String[] parameters = (String[]) object;
            if (propertyType.isArray())
            {
                Class componentType = propertyType.getComponentType();
                if (componentType.equals(String.class))
                {
                    return parameters;
                }
                Object[] array = (Object[]) Array.newInstance(componentType, parameters.length);
                for (int i = 0; i < parameters.length; i++)
                {
                    array[i] = convert(componentType, parameters[i]);
                }
                return array;
            }
            return convert(propertyType, parameters[0]);
        }

        private Object convert(Class propertyType, String parameter)
        {
            parameter = parameter.trim();
            if (parameter.equals(""))
            {
                return null;
            }
            if (propertyType.equals(String.class) || propertyType.equals(Object.class))
            {
                return parameter;
            }
            if (propertyType.equals(Character.class))
            {
                return new Character(parameter.charAt(0));
            }
            Constructor constructor = null;
            try
            {
                constructor = propertyType.getConstructor(new Class[] { String.class });
            }
            catch (Exception e)
            {
            }
            if (constructor != null)
            {
                try
                {
                    return constructor.newInstance(new Object[] { parameter });
                }
                catch (Exception e)
                {
                    throw new Danger("Cannot convert.", e);
                }
            }
            throw new Danger("Unconvertable.");
        }
    }

    public final static class NullConverter
    implements Converter
    {
        public Object convert(Class propertyType, Object object)
        {
            return object;
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