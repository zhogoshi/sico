package dev.hogoshi.sioc.handler.predefined;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import dev.hogoshi.sioc.annotation.Component;
import dev.hogoshi.sioc.annotation.Configuration;
import dev.hogoshi.sioc.annotation.PostConstruct;
import dev.hogoshi.sioc.annotation.Repository;
import dev.hogoshi.sioc.annotation.Service;
import dev.hogoshi.sioc.container.Container;
import dev.hogoshi.sioc.handler.AbstractComponentHandler;
import dev.hogoshi.sioc.handler.ComponentRegisterHandler.Phase;

public class PostConstructHandler extends AbstractComponentHandler {
    private final Container container;
    private final Set<Class<?>> initializedClasses = new HashSet<>();

    public PostConstructHandler(Container container) {
        super(20, Phase.POST_PROCESSING, Component.class, Service.class, Repository.class, Configuration.class);
        this.container = container;
    }

    @Override
    public void handle(Class<?> componentClass) {
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
            System.err.println("Error processing @PostConstruct for class: " + componentClass.getName());
            e.printStackTrace();
        }
    }
    
    private void invokePostConstructMethod(Method method, Object instance) {
        try {
            method.setAccessible(true);
            if (method.getParameterCount() == 0) {
                method.invoke(instance);
            } else {
                System.err.println("@PostConstruct method should have no parameters: " + 
                    method.getName() + " in " + instance.getClass().getName());
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Failed to invoke @PostConstruct method " + method.getName());
            e.printStackTrace();
        }
    }
} 