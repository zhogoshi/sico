package dev.hogoshi.sico.handler.predefined;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.hogoshi.sico.annotation.Component;
import dev.hogoshi.sico.annotation.Configuration;
import dev.hogoshi.sico.annotation.Repository;
import dev.hogoshi.sico.annotation.Scheduled;
import dev.hogoshi.sico.annotation.Service;
import dev.hogoshi.sico.container.Container;
import dev.hogoshi.sico.handler.AbstractComponentHandler;
import dev.hogoshi.sico.scheduler.SchedulerService;
import org.jetbrains.annotations.NotNull;

public class ScheduledHandler extends AbstractComponentHandler {
    private final SchedulerService schedulerService;
    private final Map<Class<?>, List<String>> componentTaskIds = new HashMap<>();
    
    public ScheduledHandler(Container container, SchedulerService schedulerService) {
        super(container, 50, Phase.POST_PROCESSING, Component.class, Service.class, Repository.class, Configuration.class);
        this.schedulerService = schedulerService;
    }

    @Override
    public void handle(@NotNull Class<?> componentClass) {
        try {
            Object instance = container.resolve(componentClass);
            if (instance == null) {
                return;
            }

            for (Method method : componentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Scheduled.class)) {
                    registerScheduledMethod(instance, method);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error processing @Scheduled for class: " + componentClass.getName(), e);
        }
    }
    
    private void registerScheduledMethod(Object instance, Method method) {
        Scheduled annotation = method.getAnnotation(Scheduled.class);
        
        if (method.getParameterCount() > 0) {
            throw new IllegalArgumentException("@Scheduled method must have no params: " + method.getName() + " in " + instance.getClass().getName());
        }
        
        long interval = annotation.interval();
        TimeUnit unit = annotation.unit();
        long initialDelay = annotation.initialDelay();
        boolean fixedRate = annotation.fixedRate();
        
        if (schedulerService.isRunning()) {
            String taskId = schedulerService.scheduleTask(instance, method, initialDelay, interval, unit, fixedRate);
            
            componentTaskIds.computeIfAbsent(instance.getClass(), k -> new ArrayList<>()).add(taskId);
        } else {
            throw new IllegalStateException("Unable to schedule task - scheduler service isn't running: " + instance.getClass().getName() + "." + method.getName());
        }
    }

    public void cancelScheduledTasks(Class<?> componentClass) {
        List<String> taskIds = componentTaskIds.get(componentClass);
        if (taskIds != null) {
            for (String taskId : taskIds) {
                schedulerService.cancelTask(taskId);
            }
            componentTaskIds.remove(componentClass);
        }
    }

    public void cancelAllScheduledTasks() {
        for (Class<?> componentClass : new ArrayList<>(componentTaskIds.keySet())) {
            cancelScheduledTasks(componentClass);
        }
    }
} 