package dev.hogoshi.sico.handler.predefined;

import dev.hogoshi.sico.annotation.Autowired;
import dev.hogoshi.sico.annotation.Component;
import dev.hogoshi.sico.annotation.Configuration;
import dev.hogoshi.sico.annotation.Repository;
import dev.hogoshi.sico.annotation.Service;
import dev.hogoshi.sico.container.Container;
import dev.hogoshi.sico.handler.AbstractComponentHandler;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class AutowiredHandler extends AbstractComponentHandler {
    private final Container container;

    public AutowiredHandler(Container container) {
        super(10, Phase.POST_PROCESSING, Component.class, Service.class, Repository.class, Configuration.class);
        this.container = container;
    }

    @Override
    public void handle(@NotNull Class<?> componentClass) {
        try {
            Object instance = container.resolve(componentClass);
            if (instance == null) {
                return;
            }

            for (Field field : componentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    processAutowiredField(field, instance);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing autowired fields for class: " + componentClass.getName(), e);
        }
    }

    private void processAutowiredField(Field field, Object instance) throws IllegalAccessException {
        field.setAccessible(true);
        Object dependency = container.resolve(field.getType());
        
        if (field.get(instance) == null && dependency != null) {
            field.set(instance, dependency);
        }
    }
} 