/* Copyright Alan Gutierrez 2006 */
package com.goodworkalan.stencil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

public class Okay
{
    private final List listOfValidators;

    private final DirtSimpleEvaluator evaluator;

    public Okay()
    {
        this.listOfValidators = new ArrayList();
        this.evaluator = new DirtSimpleEvaluator();
    }

    public void add(Validator validator)
    {
        listOfValidators.add(validator);
    }

    public boolean valid(Map map)
    {
        Map mapOfErrors = (Map) map.get("errors");
        if (mapOfErrors == null)
        {
            mapOfErrors = new HashMap();
            map.put("errors", mapOfErrors);
        }
        return valid(map, mapOfErrors);
    }

    public boolean valid(Recorder reporter)
    {
        return valid(reporter, reporter.getErrors());
    }

    private boolean valid(Object object, Map mapOfErrors)
    {

        boolean valid = true;
        Iterator validators = listOfValidators.iterator();
        while (validators.hasNext())
        {
            Validator validator = (Validator) validators.next();
            valid = validator.valid(evaluator, object, mapOfErrors) && valid;
        }
        return valid;
    }

    public interface Validator
    {
        public boolean valid(DirtSimpleEvaluator evaluator, Object object, Map mapOfErrors);
    }

    public static void report(Recorder recorder, String fieldName, String errorName)
    {
        put(recorder.getErrors(), fieldName, errorName);
    }

    public static void report(Map map, String fieldName, String errorName)
    {
        Map mapOfErrors = (Map) map.get("errors");
        if (mapOfErrors == null)
        {
            mapOfErrors = new HashMap();
            map.put("errors", mapOfErrors);
        }
        put(mapOfErrors, fieldName, errorName);
    }

    private static void put(Map mapOfErrors, String fieldName, String errorName)
    {
        String[] segments = fieldName.split("\\.");
        Map map = mapOfErrors;
        for (int i = 0; i < segments.length; i++)
        {
            Map subMap = (Map) map.get(segments[i]);
            if (subMap == null)
            {
                subMap = new HashMap();
                map.put(segments[i], subMap);
            }
            map = subMap;
        }
        map.put(errorName, Boolean.TRUE);
    }

    public interface Recorder
    {
        public Map getErrors();
    }

    public final static class Each
    implements Validator
    {
        private final String path;

        private final Validator validtor;

        public Each(String path, Validator validator)
        {
            this.path = path;
            this.validtor = validator;
        }

        public boolean valid(DirtSimpleEvaluator evaluator, Object object, final Map mapOfErrors)
        {
            boolean valid = true;
            Object objects = evaluator.get(object, path);
            Iterator iterator = null;
            if (objects instanceof Map)
            {
                iterator = ((Map) objects).values().iterator();
            }
            else if (objects instanceof Collection)
            {
                iterator = ((Collection) objects).iterator();
            }
            else if (objects instanceof Iterator)
            {
                iterator = (Iterator) objects;
            }
            else if (objects.getClass().isArray())
            {
                List listOfObjects = new ArrayList();
                Object[] array = (Object[]) objects;
                for (int i = 0; i < array.length; i++)
                {
                    listOfObjects.add(array[i]);
                }
                iterator = listOfObjects.iterator();
            }
            else
            {
                iterator = Collections.singleton(objects).iterator();
            }

            while (iterator.hasNext())
            {
                Object child = iterator.next();
                Map mapOfChildErrors = null;
                if (child instanceof Recorder)
                {
                    mapOfChildErrors = (Map) ((Recorder) child).getErrors();
                }
                else if (child instanceof Map)
                {
                    mapOfChildErrors = (Map) ((Map) child).get("errors");
                    if (mapOfErrors == null)
                    {
                        mapOfChildErrors = new HashMap();
                        ((Map) child).put("errors", mapOfErrors);
                    }
                }
                else
                {
                    throw new UnsupportedOperationException();
                }
                valid = validtor.valid(evaluator, child, mapOfChildErrors) && valid;
            }

            return valid;
        }
    }

    public final static class Required
    implements Validator
    {
        private final String path;

        public Required(String path)
        {
            this.path = path;
        }

        public boolean valid(DirtSimpleEvaluator evaluator, Object object, Map mapOfErrors)
        {
            Object value = evaluator.get(object, path);
            if (value != null && value.getClass().isArray())
            {
                if (((Object[]) value).length == 0)
                {
                    value = null;
                }
                else
                {
                    value = ((Object[]) value)[0];
                }
            }
            if (value == null || ((value instanceof String) && ((String) value).trim().equals("")))
            {
                put(mapOfErrors, path, "required");
                return false;
            }
            return true;
        }
    }

    public final static class Confirm
    implements Validator
    {
        private final String first;

        private final String second;

        public Confirm(String first, String second)
        {
            this.first = first;
            this.second = second;
        }

        private static Object get(DirtSimpleEvaluator evaluator, Object object, Map mapOfErrors, String field)
        {
            Object value = evaluator.get(object, field);
            if (value != null && value.getClass().isArray())
            {
                if (((Object[]) value).length == 0)
                {
                    value = null;
                }
                else
                {
                    value = ((Object[]) value)[0];
                }
            }
            if (value == null || ((value instanceof String) && ((String) value).trim().equals("")))
            {
                value = null;
                put(mapOfErrors, field, "required");
            }
            return value;
        }

        public boolean valid(DirtSimpleEvaluator evaluator, Object object, Map mapOfErrors)
        {
            Object firstValue = get(evaluator, object, mapOfErrors, first);
            Object secondValue = get(evaluator, object, mapOfErrors, second);
            if (firstValue == null || secondValue == null)
            {
                return false;
            }
            if (!firstValue.equals(secondValue))
            {
                put(mapOfErrors, first, "confirm");
                return false;
            }
            return true;
        }
    }

    public final static class IsURL
    implements Validator
    {
        private final String path;

        public IsURL(String path)
        {
            this.path = path;
        }

        public boolean valid(DirtSimpleEvaluator evaluator, Object object, Map mapOfErrors)
        {
            Object value = evaluator.get(object, path);
            if (value != null)
            {
                try
                {
                    new URL((String) value);
                }
                catch (MalformedURLException e)
                {
                    put(mapOfErrors, path, "url");
                    return false;
                }
            }
            return true;
        }
    }

    public final static class IsEmail
    implements Validator
    {
        private final String path;

        public IsEmail(String path)
        {
            this.path = path;
        }

        public boolean valid(DirtSimpleEvaluator evaluator, Object object, Map mapOfErrors)
        {
            boolean valid = true;
            Object value = evaluator.get(object, path);
            if (value != null && !((String) value).trim().equals(""))
            {
                try
                {
                    new InternetAddress((String) value, true).validate();
                }
                catch (AddressException e)
                {
                    valid = false;
                }
                if (valid)
                {
                    String domain = ((String) value).split("@")[1];
                    valid = domain.split("\\.").length > 1;
                }
                if (!valid)
                {
                    put(mapOfErrors, path, "email");
                }
            }
            return valid;
        }
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */