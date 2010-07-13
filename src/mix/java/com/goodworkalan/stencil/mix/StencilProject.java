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
                .produces("com.github.bigeasy.stencil/stencil/0.1.0.1")
                .depends()
                    .production("javax.servlet/servlet-api/2.5")
                    .production("com.github.bigeasy.reflective/reflective-getter/0.+1")
                    .production("com.github.bigeasy.permeate/permeate/0.+1")
                    .production("com.github.bigeasy.danger/danger/0.+1")
                    .production("com.github.bigeasy.diffuse/diffuse/0.+1")
                    .production("com.github.bigeasy.ilk/ilk-inject/0.+1")
                    .production("com.github.bigeasy.ilk/ilk-loader/0.+1")
                    .production("com.github.bigeasy.ilk/ilk-inject-alias/0.+1")
                    .development("org.testng/testng-jdk15/5.10")
                    .development("com.github.bigeasy.comfort-io/comfort-io/0.+1")
                    .end()
                .end()
            .end();
    }
}
