package dev.hogoshi.sioc.container;

import java.util.function.Predicate;

public interface Container {

    <T> T resolve(Class<T> clazz);

    <T> T resolve(String name, Class<T> clazz);

    void register(Class<?> clazz);

    void registerBeanDefinition(BeanDefinition beanDefinition);

    void registerBean(String name, Object instance);

    void scan(Predicate<String> filter, String... packageNames);

    void close();
}
