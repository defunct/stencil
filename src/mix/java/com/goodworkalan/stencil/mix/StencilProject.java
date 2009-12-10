package com.goodworkalan.stencil.mix;

import com.goodworkalan.go.go.Artifact;
import com.goodworkalan.mix.ProjectModule;
import com.goodworkalan.mix.builder.Builder;
import com.goodworkalan.mix.builder.JavaProject;

public class StencilProject extends ProjectModule {
    @Override
    public void build(Builder builder) {
        builder
            .cookbook(JavaProject.class)
                .produces(new Artifact("com.goodworkalan/stencil/0.1"))
                .main()
                    .depends()
                        .artifact(new Artifact("javax.servlet/servlet-api/2.5"))
                        .artifact(new Artifact("com.habitsoft/xhtml-dtd-cache/0.1"))
                        .artifact(new Artifact("xom/xom/1.1"))
                        .artifact(new Artifact("javax.mail/mail/1.4.1"))
                        .artifact(new Artifact("org.slf4j/slf4j-api/1.4.2"))
                        .end()
                    .end()
                .test()
                    .depends()
                        .artifact(new Artifact("org.slf4j/slf4j-log4j12/1.4.2"))
                        .artifact(new Artifact("log4j/log4j/1.2.14"))
                        .artifact(new Artifact("xmlunit/xmlunit/1.2"))
                        .artifact(new Artifact("org.testng/testng/5.10"))
                        .artifact(new Artifact("org.mockito/mockito-core/1.6"))
                        .end()
                    .end()
                .end()
            .end();
    }
}
