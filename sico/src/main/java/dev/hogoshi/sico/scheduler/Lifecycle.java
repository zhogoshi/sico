package dev.hogoshi.sioc.scheduler;

public interface Lifecycle {

    void start();

    void stop();

    boolean isRunning();
} 