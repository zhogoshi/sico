package dev.hogoshi.sioc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Scheduled {

    long interval() default 1;

    TimeUnit unit() default TimeUnit.SECONDS;

    long initialDelay() default 0;

    boolean fixedRate() default true;
} 