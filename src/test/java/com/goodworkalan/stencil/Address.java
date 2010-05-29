/* Copyright Alan Gutierrez 2006 */
package com.goodworkalan.stencil;

public class Address
{
    private final String street;

    private final String city;

    private final String state;

    private final String zip;

    public Address(String street, String city, String state, String zip)
    {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
    }

    public String getStreet()
    {
        return street;
    }

    public String getCity()
    {
        return city;
    }

    public String getState()
    {
        return state;
    }

    public String getZip()
    {
        return zip;
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */