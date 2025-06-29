package dev.hogoshi.sico.scheduler;

/**
 * Interface for components that have a lifecycle.
 * Components that implement this interface can be started, stopped, and checked for running status.
 */
public interface Lifecycle {
    /**
     * Starts the component.
     * This method should initialize resources and start any background processes.
     */
    void start();

    /**
     * Stops the component.
     * This method should clean up resources and stop any background processes.
     */
    void stop();

    /**
     * Checks if the component is running.
     *
     * @return true if the component is running, false otherwise
     */
    boolean isRunning();
} 