package com.goodworkalan.stringbeans.mix;

import com.goodworkalan.mix.ProjectModule;
import com.goodworkalan.mix.builder.Builder;
import com.goodworkalan.mix.cookbook.JavaProject;

/**
 * Builds the project definition for Stencil Mistakes.
 *
 * @author Alan Gutierrez
 */
public class StencilMistakesProject implements ProjectModule {
    /**
     * Build the project definition for Stencil Mistakes.
     *
     * @param builder
     *          The project builder.
     */
    public void build(Builder builder) {
        builder
            .cookbook(JavaProject.class)
                .produces("com.github.bigeasy.stencil/stencil-mistakes/0.1")
                .depends()
                    .production("com.github.bigeasy.okay/okay/0.+1")
                    .production("com.github.bigeasy.stencil/stencil/0.+1")
                    .development("org.testng/testng-jdk15/5.10")
                    .development("org.mockito/mockito-core/1.6")
                    .end()
                .end()
            .end();
    }
}
