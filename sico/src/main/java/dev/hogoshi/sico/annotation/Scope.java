package dev.hogoshi.sico.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jetbrains.annotations.NotNull;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Scope {

    Scopes value() default Scopes.SINGLETON;

    enum Scopes {
        /**
         * Single instance is created and reused
         */
        SINGLETON,
        
        /**
         * New instance is created each time the bean is requested
         */
        PROTOTYPE,

        ;
    }
} 