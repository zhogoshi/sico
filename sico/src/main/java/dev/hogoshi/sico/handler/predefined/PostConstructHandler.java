package dev.hogoshi.sico.handler.predefined;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import dev.hogoshi.sico.annotation.Component;
import dev.hogoshi.sico.annotation.Configuration;
import dev.hogoshi.sico.annotation.PostConstruct;
import dev.hogoshi.sico.annotation.Repository;
import dev.hogoshi.sico.annotation.Service;
import dev.hogoshi.sico.container.Container;
import dev.hogoshi.sico.handler.AbstractComponentHandler;
import org.jetbrains.annotations.NotNull;

public class PostConstructHandler extends AbstractComponentHandler {
    private final Set<Class<?>> initializedClasses = new HashSet<>();

    public PostConstructHandler(Container container) {
        super(container, 20, Phase.POST_PROCESSING, Component.class, Service.class, Repository.class, Configuration.class);
    }

    @Override
    public void handle(@NotNull Class<?> componentClass) {
        if (initializedClasses.contains(componentClass)) {
            return;
        }

        try {
            Object instance = container.resolve(componentClass);
            if (instance == null) {
                return;
            }

            for (Method method : componentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    invokePostConstructMethod(method, instance);
                }
            }
            
            initializedClasses.add(componentClass);
        } catch (Exception e) {
            throw new IllegalStateException("Error processing @PostConstruct for class: " + componentClass.getName(), e);
        }
    }
    
    private void invokePostConstructMethod(Method method, Object instance) {
        try {
            method.setAccessible(true);
            if (method.getParameterCount() == 0) {
                method.invoke(instance);
            } else {
                throw new IllegalStateException("@PostConstruct method should have no parameters: " + method.getName() + " in " + instance.getClass().getName());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke @PostConstruct method " + method.getName(), e);
        }
    }
} 