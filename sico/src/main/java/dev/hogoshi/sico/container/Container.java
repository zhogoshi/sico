package dev.hogoshi.sico.container;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public interface Container {

    <T> T resolve(@NotNull Class<T> clazz);

    <T> T resolve(@NotNull String name, @NotNull Class<T> clazz);

    void register(@NotNull Class<?> clazz);

    void registerBeanDefinition(@NotNull BeanDefinition beanDefinition);

    void registerBean(@NotNull String name, @NotNull Object instance);

    void scan(@NotNull Predicate<String> filter, @NotNull String... packageNames);

    void close();
}
