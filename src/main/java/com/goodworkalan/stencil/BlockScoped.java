package com.goodworkalan.stencil;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;
import javax.inject.Scope;

/**
 * Indicates that an object will last the lifetime of the current binding, whose
 * lifetime is in turn determined by the lifetime of the block in which the
 * binding was created.
 * 
 * @author Alan Gutierrez
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@Scope
@interface BlockScoped {
}
