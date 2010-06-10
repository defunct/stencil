package com.goodworkalan.stencil.mix;

import com.goodworkalan.go.go.library.Artifact;
import com.goodworkalan.mix.ProjectModule;
import com.goodworkalan.mix.builder.Builder;
import com.goodworkalan.mix.cookbook.JavaProject;

/**
 * Builds the project definition for Stencil.
 *
 * @author Alan Gutierrez
 */
public class StencilProject implements ProjectModule {
    /**
     * Build the project definition for Stencil.
     *
     * @param builder
     *          The project builder.
     */
    public void build(Builder builder) {
        builder
            .cookbook(JavaProject.class)
                .produces("com.github.bigeasy.stencil/stencil/0.1")
                .depends()
                    .production("javax.servlet/servlet-api/2.5")
                    .production("com.habitsoft/html-dtd-cache/1.0")
                    .production("xom/xom/1.1")
                    .production("com.github.bigeasy.reflective/reflective-getter/0.+1")
                    .production("com.github.bigeasy.permeate/permeate/0.+1")
                    .production("com.github.bigeasy.diffuse/diffuse/0.+1")
                    .production("com.github.bigeasy.ilk/ilk-inject/0.+1")
                    .production("com.github.bigeasy.ilk/ilk-loader/0.+1")
                    .production("javax.mail/mail/1.4.1")
                    .production("org.slf4j/slf4j-api/1.4.2")
                    .development("org.slf4j/slf4j-log4j12/1.4.2")
                    .development("log4j/log4j/1.2.14")
                    .development("xmlunit/xmlunit/1.2")
                    .development("org.testng/testng/5.10")
                    .development("org.mockito/mockito-core/1.6")
                    .end()
                .end()
            .end();
    }
}
