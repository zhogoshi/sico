package dev.hogoshi.sico.container;

import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

/**
 * Defines the core functionality of a dependency injection container.
 * A container manages the lifecycle of components, resolves dependencies, and provides access to registered beans.
 */
public interface Container {

    /**
     * Resolves a component by type.
     * 
     * @param <T> the type of component to resolve
     * @param clazz the class of the component to resolve
     * @return the component instance, or null if no component of that type exists
     * @throws IllegalStateException if the container is closed
     */
    <T> T resolve(@NotNull Class<T> clazz);

    /**
     * Resolves a component by name and type.
     * 
     * @param <T> the type of component to resolve
     * @param name the name of the component to resolve
     * @param clazz the class of the component to resolve
     * @return the component instance, or null if no component with that name and type exists
     * @throws IllegalStateException if the container is closed
     */
    <T> T resolve(@NotNull String name, @NotNull Class<T> clazz);

    /**
     * Registers a component class with the container.
     * This creates an instance of the class and processes it with the appropriate handlers.
     * 
     * @param clazz the class to register
     * @throws IllegalStateException if the container is closed or if circular dependency is detected
     * @throws RuntimeException if registration fails
     */
    void register(@NotNull Class<?> clazz);

    /**
     * Registers a bean definition with the container.
     * 
     * @param beanDefinition the bean definition to register
     * @throws IllegalStateException if the container is closed
     */
    void registerBeanDefinition(@NotNull BeanDefinition beanDefinition);

    /**
     * Registers a bean instance with the container.
     * 
     * @param name the name of the bean
     * @param instance the bean instance
     * @throws IllegalStateException if the container is closed
     */
    void registerBean(@NotNull String name, @NotNull Object instance);

    /**
     * Scans the specified packages for components and registers them in the container.
     * 
     * @param filter a predicate to filter class names during scanning
     * @param packageNames the package names to scan
     * @throws IllegalStateException if the container is closed
     */
    void scan(@NotNull Predicate<String> filter, @NotNull String... packageNames);

    /**
     * Scans the specified packages using a custom class loader and registers the found components in the container.
     * 
     * @param filter a predicate to filter class names during scanning
     * @param classLoader the class loader to use for scanning
     * @param packageNames the package names to scan
     * @throws IllegalStateException if the container is closed
     */
    void scan(@NotNull Predicate<String> filter, @NotNull ClassLoader classLoader, @NotNull String... packageNames);

    /**
     * Closes the container and releases all resources.
     * 
     * @throws IllegalStateException if an error occurs closing the container
     */
    void close();
}
