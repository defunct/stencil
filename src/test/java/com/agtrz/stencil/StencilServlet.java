/* Copyright Alan Gutierrez 2006 */
package com.agtrz.stencil;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import com.agtrz.stencil.Stencil;

/** @fixme I belong in a different test directory, not junit. */
public class StencilServlet
extends HttpServlet
{
    private static final long serialVersionUID = 20070422L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        common(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        common(request, response);
    }

    private final void required(Map map, String name)
    {
        String[] value = (String[]) map.get(name);
        if (value == null || value.length == 0 || value[0].trim().equals(""))
        {
            map.put(name + "Error", Boolean.TRUE);
        }
    }

    private void common(final HttpServletRequest request, final HttpServletResponse response)
    {
        response.setContentType("text/xml");
        try
        {
            Stencil.Template template = new Stencil.Template(getServletContext().getResourceAsStream("/index.xml"));
            Stencil.Snippit snippit = template.newSnippit("hello");
            Map mapOfParameters = request.getParameterMap();
            if (request.getMethod().equals("POST"))
            {
                required(mapOfParameters, "firstName");
                required(mapOfParameters, "lastName");
                required(mapOfParameters, "email");
            }
            snippit.bind(mapOfParameters);
            new Serializer(response.getOutputStream()).write(snippit.getDocument());
        }
        catch (ValidityException e)
        {
            throw new RuntimeException(e);
        }
        catch (ParsingException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    // private final class SubscriberModel
    // {
    // private final String firstName;
    //
    // private final String lastName;
    //
    // private final String email;
    //
    // public SubscriberModel(HttpServletRequest request)
    // {
    // this.firstName = ;
    // }
    // }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */