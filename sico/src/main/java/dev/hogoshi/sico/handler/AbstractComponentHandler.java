package dev.hogoshi.sico.handler;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import dev.hogoshi.sico.container.Container;
import lombok.Getter;

public abstract class AbstractComponentHandler implements ComponentRegisterHandler {

    @NotNull
    protected final Container container;
    
    @Getter
    @NotNull
    protected final Phase phase;
    
    @Getter
    protected final int order;
    
    @NotNull
    private final Set<Class<? extends Annotation>> supportedAnnotations;

    protected AbstractComponentHandler(@NotNull Container container, int order, @NotNull Phase phase,
                                     @NotNull Class<? extends Annotation>... supportedAnnotations) {
        this.container = container;
        this.order = order;
        this.phase = phase;
        this.supportedAnnotations = new HashSet<>(Arrays.asList(supportedAnnotations));
    }
    
    protected AbstractComponentHandler(@NotNull Container container, int order, 
                                     @NotNull Class<? extends Annotation>... supportedAnnotations) {
        this(container, order, Phase.REGISTRATION, supportedAnnotations);
    }

    @Override
    public boolean supports(@NotNull Class<?> componentClass) {
        if (supportedAnnotations.isEmpty()) {
            return true;
        }
        
        for (Class<? extends Annotation> annotationClass : supportedAnnotations) {
            if (componentClass.isAnnotationPresent(annotationClass)) {
                return true;
            }
        }
        
        return false;
    }

    @NotNull
    protected Container getContainer() {
        return container;
    }
} 