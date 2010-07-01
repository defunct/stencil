 /* Copyright Alan Gutierrez 2006 */
package com.goodworkalan.stencil;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;

import org.testng.annotations.Test;

import com.goodworkalan.comfort.io.Files;
import com.goodworkalan.ilk.Ilk;
import com.goodworkalan.ilk.inject.Injector;
import com.goodworkalan.ilk.inject.InjectorBuilder;


/**
 * Unit tests for the {@link StencilFactory} class.
 *
 * @author Alan Gutierrez
 */
public class StencilTest {
    /** Test a do nothing template. */
    @Test
    public void nothing() throws IOException {
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        StringWriter output = new StringWriter();
        stencils.stencil(new InjectorBuilder().newInjector(), URI.create("nothing.txt"), output);
        String control = slurp(getClass().getResourceAsStream("nothing.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test comments. */
    @Test
    public void comment() throws IOException {
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        StringWriter output = new StringWriter();
        stencils.stencil(new InjectorBuilder().newInjector(), URI.create("comment.txt"), output);
        String control = slurp(getClass().getResourceAsStream("nothing.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test variable assignments. */
    @Test
    public void get() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("get.txt"), output);
        String control = slurp(getClass().getResourceAsStream("get.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /**
     * Slurp the entire input stream into a single string.
     * 
     * @param in
     *            The input stream.
     * @return The contents of the stream as a string.
     * @throws IOException
     *             For any I/O error.
     */
    public String slurp(InputStream in) throws IOException {
        StringBuilder string = new StringBuilder();
        Reader reader = new InputStreamReader(in);
        int ch;
        while ((ch = reader.read()) != -1) {
            string.append((char) ch);
        }
        return string.toString();
    }

    /** Create an injector around a clique. */
    public Injector clique() {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                Clique clique = new Clique();
                clique.people.add(new Person("George", "Washington"));
                clique.people.add(new Person("John", "Adams"));
                clique.people.add(new Person("Thomas", "Jefferson"));
                clique.people.add(new Person("James", "Madison"));
                instance(clique, ilk(Clique.class), null);
            }
        });
        return newInjector.newInjector();
    }

    /** Test each. */
    @Test
    public void each() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                Clique clique = new Clique();
                clique.people.add(new Person("George", "Washington"));
                clique.people.add(new Person("John", "Adams"));
                clique.people.add(new Person("Thomas", "Jefferson"));
                clique.people.add(new Person("James", "Madison"));
                instance(clique, ilk(Clique.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = clique();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("each.txt"), output);
        String control = slurp(getClass().getResourceAsStream("each.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test if. */
    @Test
    public void testIf() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test inline if with content after the closing if. */
    @Test
    public void ifTrailing() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-trailing.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-trailing.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }
    
    /** Test inline if with content before the opening if. */
    @Test
    public void ifLeading() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-leading.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-leading.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test inline if with false value for if. */
    @Test
    public void ifFalse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-false.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-false.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }


    /** Test block if. */
    @Test
    public void ifBlock() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-block.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-block.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test block if. */
    @Test
    public void ifBlockFalse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-block.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-block-false.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test block if. */
    @Test
    public void ifIndent() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-indent.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-indent.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test block if. */
    @Test
    public void ifIndentFalse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-indent.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-indent-false.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test indented if with blank lines. */
    @Test
    public void ifIndentWhitespace() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-indent-whitespace.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-indent-whitespace.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test indented if with blank lines when false. */
    @Test
    public void ifIndentWhitespaceFalse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-indent-whitespace.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-indent-whitespace-false.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test indented if with indented trailing blank lines. */
    @Test
    public void ifIndentTrailingBlank() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-indent-trailing-blank.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-indent-trailing-blank.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test indented if with indented trailing blank lines when false. */
    @Test
    public void ifIndentTrailingBlankFalse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-indent-trailing-blank.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-indent-trailing-blank-false.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test else. */
    @Test
    public void ifElse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("George", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-else.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-else.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test else false. */
    @Test
    public void ifElseFalse() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("if-else.txt"), output);
        String control = slurp(getClass().getResourceAsStream("if-else-false.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test else false. */
    @Test
    public void stencil() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("stencil.txt"), output);
        String control = slurp(getClass().getResourceAsStream("stencil.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /** Test else false. */
    @Test
    public void entities() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person("This <is not> really & a name.", "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("entities.txt"), output);
        String actual = output.toString();
        String control = slurp(getClass().getResourceAsStream("entities.out.txt"));
        assertEquals(actual, control);
    }

    /** Test stencil indent. */
    @Test
    public void stencilIndent() throws IOException {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        Injector injector = newInjector.newInjector();
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create("stencil-indent.txt"), output);
        String control = slurp(getClass().getResourceAsStream("stencil-indent.out.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
    }

    /**
     * Run the given script with the given injector and test for the given
     * expected output.
     * 
     * @param injector
     *            The injector.
     * @param script
     *            The script.
     * @param expected
     *            The expected output.
     * @throws IOException
     *             For any I/O error.
     */
    private void test(Injector injector, String script, String expected) throws IOException {
        StencilFactory stencils = new StencilFactory();
        stencils.setBaseURI(new File(new File("."), "src/test/resources/com/goodworkalan/stencil").getAbsoluteFile().toURI());
        StringWriter output = new StringWriter();
        stencils.stencil(injector, URI.create(script), output);
        String actual = output.toString();
        String control = slurp(getClass().getResourceAsStream(expected));
        assertEquals(actual, control);
    }

    /** Test check dirty. */
    @Test
    public void checkDirty() throws IOException, InterruptedException {
        StencilFactory stencils = new StencilFactory();
        File tests = Files.file(new File("."), "src", "test", "resources", "com", "goodworkalan", "stencil");
        StringWriter output = new StringWriter();
        stencils.setBaseURI(tests.getAbsoluteFile().toURI());
        stencils.stencil(new InjectorBuilder().newInjector(), URI.create("nothing.txt"), output);
        String control = slurp(getClass().getResourceAsStream("nothing.txt"));
        String actual = output.toString();
        assertEquals(actual, control);
        output = new StringWriter();
        stencils.stencil(new InjectorBuilder().newInjector(), URI.create("nothing.txt"), output);
        control = slurp(getClass().getResourceAsStream("nothing.txt"));
        actual = output.toString();
        assertEquals(actual, control);
        stencils.setCheckDirty(true);
        assertTrue(stencils.isCheckDirty());
        output = new StringWriter();
        stencils.stencil(new InjectorBuilder().newInjector(), URI.create("nothing.txt"), output);
        control = slurp(getClass().getResourceAsStream("nothing.txt"));
        actual = output.toString();
        assertEquals(actual, control);
        Files.touch(Files.file(tests, "nothing.txt"));
        Thread.sleep(1000);
        output = new StringWriter();
        stencils.stencil(new InjectorBuilder().newInjector(), URI.create("nothing.txt"), output);
        control = slurp(getClass().getResourceAsStream("nothing.txt"));
        actual = output.toString();
        assertEquals(actual, control);
    }
    
    /** Get an injector with a person with a null first name. */
    private Injector person() {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                instance(new Person(null, "Washington"), ilk(Person.class), null);
            }
        });
        return newInjector.newInjector();
    }
    
    /** Test else if. */
    @Test
    public void elseIf() throws IOException {
        test(person(), "else-if.txt", "else-if.out.txt"); 
    }
    
    /** Test indent. */
    @Test
    public void eachIndent() throws IOException {
        test(clique(), "each-indent.txt", "each-indent.out.txt"); 
    }
    
    /** Create an array of people. */
    public Injector people() {
        InjectorBuilder newInjector = new InjectorBuilder();
        newInjector.module(new InjectorBuilder() {
            protected void build() {
                ArrayList<Person> people = new ArrayList<Person>();
                people.add(new Person("George", "Washington"));
                people.add(new Person("John", "Adams"));
                people.add(new Person("Thomas", "Jefferson"));
                people.add(new Person("James", "Madison"));
                instance(people, new Ilk<ArrayList<Person>>(){}, null);
                instance(people, new Ilk<ArrayList<Person>>(){}, Important.class);
            }
        });
        return newInjector.newInjector();
    }

    /** Test binding. */
    @Test
    public void bindings() throws IOException {
        test(people(), "bindings.txt", "bindings.out.txt");
    }
}


/* vim: set et sw=4 ts=4 ai tw=78 nowrap: */
