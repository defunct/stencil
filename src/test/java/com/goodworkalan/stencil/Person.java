/* Copyright Alan Gutierrez 2006 */
package com.goodworkalan.stencil;

public class Person
{
    private final String firstName;
    
    private final String lastName;

    public Person(String firstName, String lastName)
    {
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public String getFirstName()
    {
        return firstName;
    }
    
    public String getLastName()
    {
        return lastName;
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */