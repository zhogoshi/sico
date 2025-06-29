package dev.hogoshi.sioc.container;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import dev.hogoshi.sioc.annotation.Component;
import dev.hogoshi.sioc.annotation.Configuration;
import dev.hogoshi.sioc.annotation.Repository;
import dev.hogoshi.sioc.annotation.Scope;
import dev.hogoshi.sioc.annotation.Service;
import dev.hogoshi.sioc.handler.ComponentRegisterHandler;
import dev.hogoshi.sioc.handler.ComponentRegisterHandler.Phase;
import dev.hogoshi.sioc.handler.predefined.AutowiredHandler;
import dev.hogoshi.sioc.handler.predefined.ConfigurationHandler;
import dev.hogoshi.sioc.handler.predefined.PostConstructHandler;
import dev.hogoshi.sioc.handler.predefined.PreDestroyHandler;
import dev.hogoshi.sioc.handler.predefined.ScheduledHandler;
import dev.hogoshi.sioc.scheduler.Lifecycle;
import dev.hogoshi.sioc.scheduler.SchedulerService;

public class DefaultContainer implements Container, Lifecycle {
    private final Map<Class<?>, Object> components = new HashMap<>();
    
    private final Map<String, Object> namedComponents = new HashMap<>();
    
    private final Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
    
    private final Map<Class<?>, Set<String>> typeIndex = new HashMap<>();
    
    private final Map<BeanDefinition, Object> prototypeFactories = new HashMap<>();
    
    private final Set<Class<?>> registeredClasses = new HashSet<>();
    private final Set<Class<?>> processingClasses = new HashSet<>();
    private final List<ComponentRegisterHandler> handlers = new ArrayList<>();
    private final Set<Class<? extends Annotation>> componentAnnotations = new HashSet<>(Arrays.asList(
            Component.class, Service.class, Repository.class, Configuration.class
    ));
    private PreDestroyHandler preDestroyHandler;
    private ScheduledHandler scheduledHandler;
    private ConfigurationHandler configurationHandler;
    private SchedulerService schedulerService;
    private boolean closed = false;
    private boolean running = false;

    public DefaultContainer() {
        this(new SchedulerService());
    }
    
    public DefaultContainer(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
        handlers.add(new AutowiredHandler(this));
        handlers.add(new PostConstructHandler(this));
        handlers.add(preDestroyHandler = new PreDestroyHandler(this));
        handlers.add(scheduledHandler = new ScheduledHandler(this, schedulerService));
        handlers.add(new ConfigurationHandler(this));
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        
        if (schedulerService != null && !schedulerService.isRunning()) {
            schedulerService.start();
        }
        
        running = true;
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }
        
        if (scheduledHandler != null) {
            scheduledHandler.cancelAllScheduledTasks();
        }
        
        if (schedulerService != null && schedulerService.isRunning()) {
            schedulerService.stop();
        }
        
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public <T> T resolve(Class<T> clazz) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        Object component = components.get(clazz);
        if (component != null) {
            String scope = determineComponentScope(clazz);
            if (scope.equals(Scope.Scopes.PROTOTYPE)) {
                try {
                    return clazz.cast(createNewInstance(clazz));
                } catch (Exception e) {
                    throw new RuntimeException("Error creating prototype instance for class: " + clazz.getName(), e);
                }
            }
            return clazz.cast(component);
        }
        
        Set<String> beanNames = typeIndex.get(clazz);
        if (beanNames != null && !beanNames.isEmpty()) {
            String firstName = beanNames.iterator().next();
            return clazz.cast(namedComponents.get(firstName));
        }
        
        for (Map.Entry<Class<?>, Object> entry : components.entrySet()) {
            if (clazz.isAssignableFrom(entry.getKey())) {
                return clazz.cast(entry.getValue());
            }
        }
        
        if (isComponent(clazz) && !processingClasses.contains(clazz)) {
            register(clazz);
            return resolve(clazz);
        }
        
        return null;
    }
    
    @Override
    public <T> T resolve(String name, Class<T> clazz) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        Object namedBean = namedComponents.get(name);
        if (namedBean != null && clazz.isInstance(namedBean)) {
            return clazz.cast(namedBean);
        }
        
        BeanDefinition definition = beanDefinitions.get(name);
        if (definition != null) {
            if (definition.isPrototype()) {
                Object bean = createBeanFromDefinition(definition);
                if (bean != null && clazz.isInstance(bean)) {
                    return clazz.cast(bean);
                }
            } else if (definition.isSingleton()) {
                Object singleton = namedComponents.get(name);
                if (singleton == null) {
                    singleton = createBeanFromDefinition(definition);
                    if (singleton != null) {
                        registerBean(name, singleton);
                    }
                }
                
                if (singleton != null && clazz.isInstance(singleton)) {
                    return clazz.cast(singleton);
                }
            }
        }
        
        return null;
    }

    @Override
    public void register(Class<?> clazz) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        if (!isComponent(clazz) || registeredClasses.contains(clazz)) {
            return;
        }
        
        if (processingClasses.contains(clazz)) {
            throw new IllegalStateException("Circular dependency detected for class: " + clazz.getName());
        }
        
        processingClasses.add(clazz);

        try {
            Constructor<?> constructor = findSuitableConstructor(clazz);
            if (constructor == null) {
                throw new IllegalStateException("No suitable constructor found for class: " + clazz.getName());
            }
            
            constructor.setAccessible(true);
            
            Object instance;
            if (constructor.getParameterCount() > 0) {
                Object[] args = resolveConstructorParameters(constructor);
                instance = constructor.newInstance(args);
            } else {
                instance = constructor.newInstance();
            }
            
            String name = determineComponentName(clazz);
            
            String scope = determineComponentScope(clazz);
            
            BeanDefinition definition = BeanDefinition.forClass(name, clazz, scope, true);
            registerBeanDefinition(definition);
            
            registerBean(name, instance);
            components.put(clazz, instance);
            registeredClasses.add(clazz);
            
            processHandlersForPhase(clazz, Phase.REGISTRATION);
            
        } catch (Exception e) {
            throw new RuntimeException("Error registering class: " + clazz.getName(), e);
        } finally {
            processingClasses.remove(clazz);
        }
    }
    
    @Override
    public void registerBeanDefinition(BeanDefinition beanDefinition) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        String name = beanDefinition.getName();
        Class<?> type = beanDefinition.getBeanClass();
        
        beanDefinitions.put(name, beanDefinition);
        
        typeIndex.computeIfAbsent(type, k -> new HashSet<>()).add(name);
        
        if (beanDefinition.isFactoryMethod() && beanDefinition.isPrototype()) {
            prototypeFactories.put(beanDefinition, beanDefinition.getDeclaringInstance());
        }
        
        if (beanDefinition.isSingleton() && beanDefinition.isAutowireCandidate() && 
            !namedComponents.containsKey(name)) {
            
            Object instance = createBeanFromDefinition(beanDefinition);
            if (instance != null) {
                registerBean(name, instance);
            }
        }
    }
    
    @Override
    public void registerBean(String name, Object instance) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        if (name == null || instance == null) {
            return;
        }
        
        namedComponents.put(name, instance);
        
        Class<?> type = instance.getClass();
        typeIndex.computeIfAbsent(type, k -> new HashSet<>()).add(name);
        
        if (!components.containsKey(type)) {
            components.put(type, instance);
        }
    }
    
    private Object createBeanFromDefinition(BeanDefinition definition) {
        try {
            if (definition.isFactoryMethod()) {
                Method factoryMethod = definition.getFactoryMethod();
                factoryMethod.setAccessible(true);
                
                Object factoryInstance = definition.getDeclaringInstance();
                
                Parameter[] parameters = factoryMethod.getParameters();
                Object[] args = new Object[parameters.length];
                
                for (int i = 0; i < parameters.length; i++) {
                    Class<?> paramType = parameters[i].getType();
                    args[i] = resolve(paramType);
                    
                    if (args[i] == null) {
                        System.err.println("Failed to resolve dependency of type " + paramType.getName() + 
                            " for bean factory method: " + factoryMethod.getName());
                    }
                }
                
                return factoryMethod.invoke(factoryInstance, args);
            } else {
                Constructor<?> constructor = findSuitableConstructor(definition.getBeanClass());
                if (constructor == null) {
                    return null;
                }
                
                constructor.setAccessible(true);
                
                if (constructor.getParameterCount() > 0) {
                    Object[] args = resolveConstructorParameters(constructor);
                    return constructor.newInstance(args);
                } else {
                    return constructor.newInstance();
                }
            }
        } catch (Exception e) {
            System.err.println("Error creating bean: " + definition.getName());
            e.printStackTrace();
            return null;
        }
    }
    
    private String determineComponentName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Component.class)) {
            String name = clazz.getAnnotation(Component.class).value();
            if (!name.isEmpty()) {
                return name;
            }
        }
        
        if (clazz.isAnnotationPresent(Service.class)) {
            String name = clazz.getAnnotation(Service.class).value();
            if (!name.isEmpty()) {
                return name;
            }
        }
        
        if (clazz.isAnnotationPresent(Repository.class)) {
            String name = clazz.getAnnotation(Repository.class).value();
            if (!name.isEmpty()) {
                return name;
            }
        }
        
        if (clazz.isAnnotationPresent(Configuration.class)) {
            String name = clazz.getAnnotation(Configuration.class).value();
            if (!name.isEmpty()) {
                return name;
            }
        }
        
        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
    
    private String determineComponentScope(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Scope.class)) {
            return clazz.getAnnotation(Scope.class).value();
        }
        
        return Scope.Scopes.SINGLETON;
    }
    
    private Constructor<?> findSuitableConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(dev.hogoshi.sioc.annotation.Autowired.class)) {
                return constructor;
            }
        }
        
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            if (constructors.length > 0) {
                return constructors[0];
            }
        }
        
        return null;
    }
    
    private Object[] resolveConstructorParameters(Constructor<?> constructor) {
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Class<?> paramType = parameters[i].getType();
            args[i] = resolve(paramType);
            
            if (args[i] == null) {
                throw new IllegalStateException("Failed to resolve dependency of type " + paramType.getName() + 
                    " for constructor of " + constructor.getDeclaringClass().getName());
            }
        }
        
        return args;
    }
    
    private boolean isComponent(Class<?> clazz) {
        for (Class<? extends Annotation> annotationType : componentAnnotations) {
            if (clazz.isAnnotationPresent(annotationType)) {
                return true;
            }
        }
        return false;
    }
    
    private void processHandlersForPhase(Class<?> clazz, Phase phase) {
        Collections.sort(handlers);
        
        for (ComponentRegisterHandler handler : handlers) {
            if (handler.getPhase() == phase && handler.supports(clazz)) {
                handler.handle(clazz);
            }
        }
    }

    @Override
    public void scan(Predicate<String> filter, String... packageNames) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        for (String packageName : packageNames) {
            try {
                scanPackage(packageName, filter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        for (Class<?> clazz : new HashSet<>(registeredClasses)) {
            processHandlersForPhase(clazz, Phase.POST_PROCESSING);
        }
    }
    
    @Override
    public void close() {
        if (closed) {
            return;
        }
        
        if (running) {
            stop();
        }
        
        try {
            if (preDestroyHandler != null) {
                preDestroyHandler.executeAllPreDestroy();
            }
            
            components.clear();
            namedComponents.clear();
            beanDefinitions.clear();
            typeIndex.clear();
            registeredClasses.clear();
            
            closed = true;
        } catch (Exception e) {
            System.err.println("Error closing container");
            e.printStackTrace();
        }
    }
    
    private void scanPackage(String packageName, Predicate<String> filter) throws IOException, ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            scanDirectory(directory, packageName, filter);
        }
    }
    
    private void scanDirectory(File directory, String packageName, Predicate<String> filter) throws ClassNotFoundException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanDirectory(file, packageName + "." + file.getName(), filter);
                    } else if (file.getName().endsWith(".class")) {
                        String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                        if (filter.test(className)) {
                            try {
                                Class<?> clazz = Class.forName(className);
                                if (isComponent(clazz)) {
                                    register(clazz);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
    
    public void addHandler(ComponentRegisterHandler handler) {
        handlers.add(handler);
    }
    
    public void removeHandler(ComponentRegisterHandler handler) {
        handlers.remove(handler);
    }

    private Object createNewInstance(Class<?> clazz) throws Exception {
        Constructor<?> constructor = findSuitableConstructor(clazz);
        if (constructor == null) {
            throw new IllegalStateException("No suitable constructor found for class: " + clazz.getName());
        }
        
        constructor.setAccessible(true);
        
        Object instance;
        if (constructor.getParameterCount() > 0) {
            Object[] args = resolveConstructorParameters(constructor);
            instance = constructor.newInstance(args);
        } else {
            instance = constructor.newInstance();
        }
        
        for (ComponentRegisterHandler handler : handlers) {
            if (handler.getPhase() == Phase.REGISTRATION && handler.supports(clazz)) {
                handler.handle(clazz);
            }
        }
        
        return instance;
    }
} 