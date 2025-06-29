package dev.hogoshi.sico.handler.predefined;

import dev.hogoshi.sico.annotation.Autowired;
import dev.hogoshi.sico.annotation.Component;
import dev.hogoshi.sico.annotation.Configuration;
import dev.hogoshi.sico.annotation.Repository;
import dev.hogoshi.sico.annotation.Service;
import dev.hogoshi.sico.container.Container;
import dev.hogoshi.sico.handler.AbstractComponentHandler;
import dev.hogoshi.sico.handler.ComponentRegisterHandler.Phase;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class AutowiredHandler extends AbstractComponentHandler {

    public AutowiredHandler(@NotNull Container container) {
        super(container, 10, Phase.POST_PROCESSING, Component.class, Service.class, Repository.class, Configuration.class);
    }

    @Override
    public void handle(@NotNull Class<?> componentClass) {
        try {
            Object instance = getContainer().resolve(componentClass);
            if (instance == null) {
                return;
            }

            for (Field field : componentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    
                    if (field.get(instance) != null) {
                        continue;
                    }
                    
                    Object dependency = getContainer().resolve(field.getType());
                    
                    if (dependency != null) {
                        field.set(instance, dependency);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 