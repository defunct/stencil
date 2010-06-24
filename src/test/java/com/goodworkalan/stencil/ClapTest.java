package com.goodworkalan.stencil;

import static org.testng.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.testng.annotations.Test;

import com.goodworkalan.stencil.clap.Handler;

/**
 * Unit tests for the CLAP {@link Handler} class.
 * 
 * @author Alan Gutierrez
 */
public class ClapTest {
    /** Test the <code>ResourceResolver</code> interface. */
    @Test
    public void clap() throws IOException {
        Handler clap = new Handler();
        URL url = clap.getURL(null, URI.create("clap://thread/com/goodworkalan/stencil/clap/hello.txt"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        assertEquals(reader.readLine(), "Hello, World!");
    }
}
