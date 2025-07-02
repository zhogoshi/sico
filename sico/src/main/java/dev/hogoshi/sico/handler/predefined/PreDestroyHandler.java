package dev.hogoshi.sico.handler.predefined;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dev.hogoshi.sico.annotation.Component;
import dev.hogoshi.sico.annotation.Configuration;
import dev.hogoshi.sico.annotation.PreDestroy;
import dev.hogoshi.sico.annotation.Repository;
import dev.hogoshi.sico.annotation.Service;
import dev.hogoshi.sico.container.Container;
import dev.hogoshi.sico.handler.AbstractComponentHandler;
import org.jetbrains.annotations.NotNull;

public class PreDestroyHandler extends AbstractComponentHandler {
    private final Map<Class<?>, Set<Method>> preDestroyMethods = new HashMap<>();
    private final Set<Class<?>> processedClasses = new HashSet<>();

    public PreDestroyHandler(Container container) {
        super(container, 10, Phase.REGISTRATION, Component.class, Service.class, Repository.class, Configuration.class);
    }

    @Override
    public void handle(@NotNull Class<?> componentClass) {
        if (processedClasses.contains(componentClass)) {
            return;
        }

        try {
            Set<Method> methods = new HashSet<>();
            for (Method method : componentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PreDestroy.class)) {
                    methods.add(method);
                }
            }
            
            if (!methods.isEmpty()) {
                preDestroyMethods.put(componentClass, methods);
            }
            
            processedClasses.add(componentClass);
        } catch (Exception e) {
            throw new IllegalStateException("Error processing @PreDestroy for class: " + componentClass.getName(), e);
        }
    }

    public void executePreDestroy(Class<?> componentClass) {
        Set<Method> methods = preDestroyMethods.get(componentClass);
        if (methods == null || methods.isEmpty()) {
            return;
        }
        
        Object instance = container.resolve(componentClass);
        if (instance == null) {
            return;
        }
        
        for (Method method : methods) {
            invokePreDestroyMethod(method, instance);
        }
    }

    public void executeAllPreDestroy() {
        for (Class<?> componentClass : preDestroyMethods.keySet()) {
            executePreDestroy(componentClass);
        }
    }
    
    private void invokePreDestroyMethod(Method method, Object instance) {
        try {
            if (method.getParameterCount() == 0) {
                MethodHandle methodHandle = MethodHandles.lookup().unreflect(method).bindTo(instance);
                methodHandle.invoke();
            } else {
                throw new IllegalStateException("@PreDestroy method should have no parameters: " + method.getName() + " in " + instance.getClass().getName());
            }
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to invoke @PreDestroy method " + method.getName(), e);
        }
    }
} 