/* Copyright Alan Gutierrez 2006 */
package com.goodworkalan.stencil;

/**
 * An example person object.
 * 
 * @author Alan Gutierrez
 */
public class Person {
    /** The first name. */
    private final String firstName;

    /** The last name. */
    private final String lastName;

    /**
     * Create a new person.
     * 
     * @param firstName
     *            The first name.
     * @param lastName
     *            The last name.
     */
    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Get the first name.
     * 
     * @return The first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Get the last name.
     * 
     * @return The last name.
     */
    public String getLastName() {
        return lastName;
    }
}

/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */