package dev.hogoshi.sioc.scheduler;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SchedulerService implements Lifecycle {
    private ScheduledExecutorService executor;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new HashMap<>();
    private boolean running = false;
    private final int poolSize;
    private ClassLoader contextClassLoader;
    
    public SchedulerService() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    public SchedulerService(int poolSize) {
        this.poolSize = poolSize;
    }
    
    @Override
    public void start() {
        if (running) {
            return;
        }
        
        contextClassLoader = Thread.currentThread().getContextClassLoader();
        executor = Executors.newScheduledThreadPool(poolSize);
        running = true;
    }
    
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(false);
        }
        scheduledTasks.clear();
        
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        running = false;
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }

    public String scheduleTask(
            Object instance, 
            Method method, 
            long initialDelay, 
            long interval, 
            TimeUnit unit, 
            boolean fixedRate) {
        
        if (!running) {
            throw new IllegalStateException("Scheduler service is not running");
        }
        
        String taskId = instance.getClass().getName() + "." + method.getName() + "-" + System.nanoTime();
        
        Runnable task = () -> {
            try {
                if (contextClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                }
                
                method.setAccessible(true);
                method.invoke(instance);
            } catch (Exception e) {
                System.err.println("Error executing scheduled task: " + method.getName());
                e.printStackTrace();
            }
        };
        
        ScheduledFuture<?> future;
        if (fixedRate) {
            future = executor.scheduleAtFixedRate(task, initialDelay, interval, unit);
        } else {
            future = executor.scheduleWithFixedDelay(task, initialDelay, interval, unit);
        }
        
        scheduledTasks.put(taskId, future);
        
        return taskId;
    }

    public boolean cancelTask(String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            return true;
        }
        return false;
    }

    public int getTaskCount() {
        return scheduledTasks.size();
    }
} 