package dev.hogoshi.sico;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import dev.hogoshi.sico.container.Container;
import dev.hogoshi.sico.container.DefaultContainer;
import dev.hogoshi.sico.handler.ComponentRegisterHandler;
import dev.hogoshi.sico.scheduler.Lifecycle;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Main entry point for the Sico dependency injection container.
 * This singleton class provides access to the container and manages component scanning and registration.
 * It implements the {@link Lifecycle} interface to allow proper startup and shutdown.
 */
public class Sico implements Lifecycle {

    @Getter
    private static final Sico instance = new Sico();

    @Getter @NotNull
    private final Container container;

    @NotNull
    private final Map<String, Container> containers = new ConcurrentHashMap<>();

    /**
     * Creates a new Sico instance with a default container.
     * Protected constructor to ensure singleton pattern is followed.
     */
    protected Sico() {
        this.container = new DefaultContainer();
    }

    /**
     * Starts the container and any registered components that implement {@link Lifecycle}.
     */
    @Override
    public void start() {
        if (container instanceof Lifecycle) {
            ((Lifecycle) container).start();
        }
    }
    
    /**
     * Stops the container and any registered components that implement {@link Lifecycle}.
     */
    @Override
    public void stop() {
        if (container instanceof Lifecycle) {
            ((Lifecycle) container).stop();
        }
    }
    
    /**
     * Checks if the container is running.
     *
     * @return true if the container is running, false otherwise
     */
    @Override
    public boolean isRunning() {
        if (container instanceof Lifecycle) {
            return ((Lifecycle) container).isRunning();
        }
        return false;
    }

    /**
     * Scans the specified packages for components and registers them in the container.
     *
     * @param packageNames the package names to scan
     */
    public void scan(@NotNull String... packageNames) {
        container.scan(s -> true, packageNames);
    }

    /**
     * Scans the specified packages for components that match the filter and registers them in the container.
     *
     * @param filter a predicate to filter class names during scanning
     * @param packageNames the package names to scan
     */
    public void scan(@NotNull Predicate<String> filter, @NotNull String... packageNames) {
        container.scan(filter, packageNames);
    }

    /**
     * Scans the specified packages using a custom class loader and registers the found components in the container.
     *
     * @param classLoader the class loader to use for scanning
     * @param packageNames the package names to scan
     */
    public void scan(@NotNull ClassLoader classLoader, @NotNull String... packageNames) {
        container.scan(s -> true, classLoader, packageNames);
    }

    /**
     * Scans the specified packages using a custom class loader, applying the filter,
     * and registers matching components in the container.
     *
     * @param filter a predicate to filter class names during scanning
     * @param classLoader the class loader to use for scanning
     * @param packageNames the package names to scan
     */
    public void scan(@NotNull Predicate<String> filter, @NotNull ClassLoader classLoader, @NotNull String... packageNames) {
        container.scan(filter, classLoader, packageNames);
    }

    /**
     * Adds a named container to the registry.
     * If the container implements {@link Lifecycle} and Sico is running, the container will be started.
     *
     * @param name the name of the container
     * @param container the container instance to add
     */
    public void addContainer(@NotNull String name, @NotNull Container container) {
        containers.put(name, container);

        if (container instanceof Lifecycle && isRunning()) {
            ((Lifecycle) container).start();
        }
    }

    /**
     * Gets a container by name.
     *
     * @param name the name of the container to retrieve
     * @return the container, or null if no container with that name exists
     */
    @Nullable
    public Container getContainer(@NotNull String name) {
        return containers.get(name);
    }
    
    /**
     * Resolves a component by type.
     *
     * @param <T> the type of component to resolve
     * @param clazz the class of the component to resolve
     * @return the component instance, or null if no component of that type exists
     */
    @Nullable
    public <T> T resolve(@NotNull Class<T> clazz) {
        return container.resolve(clazz);
    }

    /**
     * Registers a component class with the container.
     *
     * @param clazz the class to register
     * @throws RuntimeException if registration fails
     */
    public void register(@NotNull Class<?> clazz) {
        container.register(clazz);
    }

    /**
     * Adds a component register handler to the container.
     *
     * @param handler the handler to add
     */
    public void addHandler(@NotNull ComponentRegisterHandler handler) {
        if (container instanceof DefaultContainer) {
            ((DefaultContainer) container).addHandler(handler);
        }
    }

    /**
     * Removes a component register handler from the container.
     *
     * @param handler the handler to remove
     */
    public void removeHandler(@NotNull ComponentRegisterHandler handler) {
        if (container instanceof DefaultContainer) {
            ((DefaultContainer) container).removeHandler(handler);
        }
    }

    /**
     * Closes the container and releases all resources.
     */
    public void close() {
        container.close();
    }

    /**
     * Resolves a component by name and type.
     *
     * @param <T> the type of component to resolve
     * @param name the name of the component to resolve
     * @param clazz the class of the component to resolve
     * @return the component instance, or null if no component with that name and type exists
     */
    @Nullable
    public <T> T resolve(@NotNull String name, @NotNull Class<T> clazz) {
        return container.resolve(name, clazz);
    }
}