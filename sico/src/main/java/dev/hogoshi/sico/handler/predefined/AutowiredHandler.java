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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
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

            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(componentClass, MethodHandles.lookup());
            
            for (Field field : componentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    try {
                        VarHandle varHandle = lookup.unreflectVarHandle(field);
                        
                        if (varHandle.get(instance) != null) {
                            continue;
                        }
                        
                        Object dependency = getContainer().resolve(field.getType());
                        
                        if (dependency != null) {
                            varHandle.set(instance, dependency);
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("Failed to access field: " + field.getName(), e);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 