package dev.hogoshi.sioc;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import dev.hogoshi.sioc.container.Container;
import dev.hogoshi.sioc.container.DefaultContainer;
import dev.hogoshi.sioc.handler.ComponentRegisterHandler;
import dev.hogoshi.sioc.scheduler.Lifecycle;
import lombok.Getter;

public class Sico implements Lifecycle {

    @Getter
    private static final Sico instance = new Sico();

    @Getter
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

    public void scan(String... packageNames) {
        container.scan(s -> true, packageNames);
    }

    public void scan(Predicate<String> filter, String... packageNames) {
        container.scan(filter, packageNames);
    }

    public void addContainer(String name, Container container) {
        containers.put(name, container);

        if (container instanceof Lifecycle && isRunning()) {
            ((Lifecycle) container).start();
        }
    }

    public Container getContainer(String name) {
        return containers.get(name);
    }
    
    public <T> T resolve(Class<T> clazz) {
        return container.resolve(clazz);
    }

    public void register(Class<?> clazz) {
        container.register(clazz);
    }

    public void addHandler(ComponentRegisterHandler handler) {
        if (container instanceof DefaultContainer) {
            ((DefaultContainer) container).addHandler(handler);
        }
    }

    public void removeHandler(ComponentRegisterHandler handler) {
        if (container instanceof DefaultContainer) {
            ((DefaultContainer) container).removeHandler(handler);
        }
    }

    public void close() {
        container.close();
    }

    public <T> T resolve(String name, Class<T> clazz) {
        return container.resolve(name, clazz);
    }
}