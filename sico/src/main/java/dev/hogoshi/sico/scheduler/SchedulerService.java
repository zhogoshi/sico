package dev.hogoshi.sico.scheduler;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Service that manages scheduled tasks.
 * This service uses a thread pool to execute tasks at specified intervals.
 * It implements the Lifecycle interface to allow for proper initialization and shutdown.
 */
public class SchedulerService implements Lifecycle {
    private ScheduledExecutorService executor;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private volatile boolean running = false;
    private final int poolSize;
    private ClassLoader contextClassLoader;
    
    /**
     * Creates a new scheduler service with a thread pool size equal to the number of available processors.
     */
    public SchedulerService() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates a new scheduler service with the specified thread pool size.
     * 
     * @param poolSize the size of the thread pool
     */
    public SchedulerService(int poolSize) {
        this.poolSize = poolSize;
    }
    
    /**
     * Starts the scheduler service by initializing the thread pool.
     */
    @Override
    public void start() {
        if (running) {
            return;
        }
        
        contextClassLoader = Thread.currentThread().getContextClassLoader();
        executor = Executors.newScheduledThreadPool(poolSize);
        running = true;
    }
    
    /**
     * Stops the scheduler service by shutting down the thread pool and canceling all scheduled tasks.
     */
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
    
    /**
     * Checks if the scheduler service is running.
     * 
     * @return true if the service is running, false otherwise
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Schedules a task to be executed periodically.
     * 
     * @param instance the instance on which to invoke the method
     * @param method the method to invoke
     * @param initialDelay the initial delay before the first execution
     * @param interval the interval between executions
     * @param unit the time unit for the initial delay and interval
     * @param fixedRate whether to use fixed rate or fixed delay execution
     * @return the task ID
     * @throws IllegalStateException if the scheduler service is not running
     */
    public @NotNull String scheduleTask(
            @NotNull Object instance,
            @NotNull Method method,
            long initialDelay, 
            long interval,
            @NotNull TimeUnit unit,
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
                throw new SchedulerException("Error executing scheduled task: " + method.getName(), e);
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

    /**
     * Cancels a scheduled task.
     * 
     * @param taskId the ID of the task to cancel
     * @return true if the task was canceled, false if the task ID was not found
     */
    public boolean cancelTask(@NotNull String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            return true;
        }
        return false;
    }

    /**
     * Gets the number of scheduled tasks.
     * 
     * @return the number of scheduled tasks
     */
    public int getTaskCount() {
        return scheduledTasks.size();
    }
    
    /**
     * Exception thrown when an error occurs executing a scheduled task.
     */
    public static class SchedulerException extends RuntimeException {
        public SchedulerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 