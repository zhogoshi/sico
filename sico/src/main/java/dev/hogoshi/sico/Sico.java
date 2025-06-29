package dev.hogoshi.sico;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import dev.hogoshi.sico.container.Container;
import dev.hogoshi.sico.container.DefaultContainer;
import dev.hogoshi.sico.handler.ComponentRegisterHandler;
import dev.hogoshi.sico.scheduler.Lifecycle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class Sico implements Lifecycle {

    @Getter
    private static final Sico instance = new Sico();

    @Getter @NotNull
    private final Container container;

    private final Map<String, Container> containers = new HashMap<>();

    protected Sico() {
        this.container = new DefaultContainer();
    }

    @Override
    public void start() {
        if (container instanceof Lifecycle) {
            ((Lifecycle) container).start();
        }
    }
    
    @Override
    public void stop() {
        if (container instanceof Lifecycle) {
            ((Lifecycle) container).stop();
        }
    }
    
    @Override
    public boolean isRunning() {
        if (container instanceof Lifecycle) {
            return ((Lifecycle) container).isRunning();
        }
        return false;
    }

    public void scan(@NotNull String... packageNames) {
        container.scan(s -> true, packageNames);
    }

    public void scan(@NotNull Predicate<String> filter, @NotNull String... packageNames) {
        container.scan(filter, packageNames);
    }

    public void addContainer(@NotNull String name, @NotNull Container container) {
        containers.put(name, container);

        if (container instanceof Lifecycle && isRunning()) {
            ((Lifecycle) container).start();
        }
    }

    public Container getContainer(@NotNull String name) {
        return containers.get(name);
    }
    
    public <T> T resolve(@NotNull Class<T> clazz) {
        return container.resolve(clazz);
    }

    public void register(@NotNull Class<?> clazz) {
        container.register(clazz);
    }

    public void addHandler(@NotNull ComponentRegisterHandler handler) {
        if (container instanceof DefaultContainer) {
            ((DefaultContainer) container).addHandler(handler);
        }
    }

    public void removeHandler(@NotNull ComponentRegisterHandler handler) {
        if (container instanceof DefaultContainer) {
            ((DefaultContainer) container).removeHandler(handler);
        }
    }

    public void close() {
        container.close();
    }

    public <T> T resolve(@NotNull String name, @NotNull Class<T> clazz) {
        return container.resolve(name, clazz);
    }
}