package dev.hogoshi.sioc.handler.predefined;

import dev.hogoshi.sioc.annotation.Bean;
import dev.hogoshi.sioc.annotation.Configuration;
import dev.hogoshi.sioc.annotation.Scope;
import dev.hogoshi.sioc.container.BeanDefinition;
import dev.hogoshi.sioc.container.Container;
import dev.hogoshi.sioc.handler.AbstractComponentHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationHandler extends AbstractComponentHandler {
    private final Container container;
    private final Map<String, Object> singletonBeanCache = new HashMap<>();

    public ConfigurationHandler(Container container) {
        super(15, Phase.REGISTRATION, Configuration.class);
        this.container = container;
    }

    @Override
    public void handle(Class<?> componentClass) {
        if (!componentClass.isAnnotationPresent(Configuration.class)) {
            return;
        }

        try {
            Object configInstance = container.resolve(componentClass);
            if (configInstance == null) {
                throw new IllegalStateException("Failed to resolve configuration class: " + componentClass.getName());
            }

            processBeanMethods(configInstance, componentClass);
        } catch (Exception e) {
            throw new RuntimeException("Error processing @Configuration class: " + componentClass.getName(), e);
        }
    }

    private void processBeanMethods(Object configInstance, Class<?> configClass) {
        List<BeanDefinition> beanDefinitions = new ArrayList<>();
        
        for (Method method : configClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Bean.class)) {
                BeanDefinition definition = createBeanDefinition(configInstance, method);
                beanDefinitions.add(definition);
                container.registerBeanDefinition(definition);
            }
        }
        
        for (BeanDefinition definition : beanDefinitions) {
            if (definition.isSingleton() && definition.isAutowireCandidate()) {
                Object bean = createAndRegisterBean(definition);
                if (bean != null) {
                    singletonBeanCache.put(definition.getName(), bean);
                    container.registerBean(definition.getName(), bean);
                }
            }
        }
    }

    private BeanDefinition createBeanDefinition(Object configInstance, Method method) {
        Bean beanAnnotation = method.getAnnotation(Bean.class);
        
        String beanName = beanAnnotation.name().isEmpty() ? method.getName() : beanAnnotation.name();
        
        Class<?> beanType = method.getReturnType();
        
        String scope = Scope.Scopes.SINGLETON;
        if (method.isAnnotationPresent(Scope.class)) {
            scope = method.getAnnotation(Scope.class).value();
        }
        
        return BeanDefinition.forMethod(
            beanName, 
            beanType, 
            scope, 
            beanAnnotation.autowireCandidate(),
            configInstance, 
            method
        );
    }
    
    private Object createAndRegisterBean(BeanDefinition beanDefinition) {
        try {
            Method factoryMethod = beanDefinition.getFactoryMethod();
            factoryMethod.setAccessible(true);
            
            Object[] args = resolveDependencies(factoryMethod);
            
            return factoryMethod.invoke(beanDefinition.getDeclaringInstance(), args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error creating bean from factory method: " + beanDefinition.getName(), e);
        }
    }
    
    private Object[] resolveDependencies(Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length == 0) {
            return new Object[0];
        }
        
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            
            args[i] = findBeanByType(paramType);
            
            if (args[i] == null) {
                args[i] = container.resolve(paramType);
            }
            
            if (args[i] == null) {
                throw new IllegalStateException("Failed to resolve dependency of type " + paramType.getName() + 
                    " for bean factory method: " + method.getName());
            }
        }
        
        return args;
    }
    
    private Object findBeanByType(Class<?> type) {
        for (Object bean : singletonBeanCache.values()) {
            if (type.isInstance(bean)) {
                return bean;
            }
        }
        return null;
    }
} 