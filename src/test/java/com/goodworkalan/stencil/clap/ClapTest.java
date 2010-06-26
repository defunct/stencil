package com.goodworkalan.stencil.clap;

import static org.testng.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.testng.annotations.Test;

/**
 * Unit tests for the CLAP {@link Handler} class.
 * 
 * @author Alan Gutierrez
 */
public class ClapTest {
    /** Test the CLAP handler. */
    @Test
    public void clap() throws IOException {
        Handler clap = new Handler();
        URL url = clap.getURL(null, URI.create("clap://thread/com/goodworkalan/stencil/clap/hello.txt"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        assertEquals(reader.readLine(), "Hello, World!");
    }
    
    /** Test parse URL. */
    @Test
    public void parseURL() throws MalformedURLException {
        new URL(null, "clap://thread/file.txt", new Handler());
        new URL(null, "clap://system/file.txt", new Handler());
        new URL(null, "clap://class/file.txt", new Handler());
    }
    
    /** Test failed parse URL. */
    @Test(expectedExceptions = MalformedURLException.class)
    public void failedParseURL() throws MalformedURLException {
        new URL(null, "clap://steve/file.txt", new Handler());
    }
    
    /** Test choosing the class loader. */
    @Test
    public void chooseClassLoader() {
        Handler clap = new Handler();
        assertSame(clap.chooseClassLoader("thread"), Thread.currentThread().getContextClassLoader());
        assertSame(clap.chooseClassLoader("system"), ClassLoader.getSystemClassLoader());
        assertSame(clap.chooseClassLoader("steve"), clap.getClass().getClassLoader());
    }
}
