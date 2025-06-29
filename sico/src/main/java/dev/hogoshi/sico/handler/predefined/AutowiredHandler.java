package dev.hogoshi.sioc.handler.predefined;

import dev.hogoshi.sioc.annotation.Autowired;
import dev.hogoshi.sioc.annotation.Component;
import dev.hogoshi.sioc.annotation.Configuration;
import dev.hogoshi.sioc.annotation.Repository;
import dev.hogoshi.sioc.annotation.Service;
import dev.hogoshi.sioc.container.Container;
import dev.hogoshi.sioc.handler.AbstractComponentHandler;

import java.lang.reflect.Field;

public class AutowiredHandler extends AbstractComponentHandler {
    private final Container container;

    public AutowiredHandler(Container container) {
        super(10, Phase.POST_PROCESSING, Component.class, Service.class, Repository.class, Configuration.class);
        this.container = container;
    }

    @Override
    public void handle(Class<?> componentClass) {
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