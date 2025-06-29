package dev.hogoshi.sico.handler;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractComponentHandler implements ComponentRegisterHandler {
    private final Set<Class<? extends Annotation>> supportedAnnotations;
    private final int priority;
    private final Phase phase;

    @SafeVarargs
    protected AbstractComponentHandler(int priority, Class<? extends Annotation>... supportedAnnotations) {
        this(priority, Phase.REGISTRATION, supportedAnnotations);
    }
    
    @SafeVarargs
    protected AbstractComponentHandler(int priority, Phase phase, Class<? extends Annotation>... supportedAnnotations) {
        this.priority = priority;
        this.phase = phase;
        this.supportedAnnotations = new HashSet<>(Arrays.asList(supportedAnnotations));
    }

    @Override
    public boolean supports(@NotNull Class<?> componentClass) {
        if (supportedAnnotations.isEmpty()) {
            return true;
        }
        
        for (Class<? extends Annotation> annotation : supportedAnnotations) {
            if (componentClass.isAnnotationPresent(annotation)) {
                return true;
            }
        }
        
        return false;
    }

    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public @NotNull Phase getPhase() {
        return phase;
    }
} 