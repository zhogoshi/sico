package dev.hogoshi.sico.scheduler;

public interface Lifecycle {

    void start();

    void stop();

    boolean isRunning();
} 