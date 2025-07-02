package dev.hogoshi.sico.container;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.hogoshi.sico.annotation.Component;
import dev.hogoshi.sico.annotation.Configuration;
import dev.hogoshi.sico.annotation.Repository;
import dev.hogoshi.sico.annotation.Scope;
import dev.hogoshi.sico.annotation.Service;
import dev.hogoshi.sico.handler.ComponentRegisterHandler;
import dev.hogoshi.sico.handler.ComponentRegisterHandler.Phase;
import dev.hogoshi.sico.handler.predefined.AutowiredHandler;
import dev.hogoshi.sico.handler.predefined.ConfigurationHandler;
import dev.hogoshi.sico.handler.predefined.PostConstructHandler;
import dev.hogoshi.sico.handler.predefined.PreDestroyHandler;
import dev.hogoshi.sico.handler.predefined.ScheduledHandler;
import dev.hogoshi.sico.scheduler.Lifecycle;
import dev.hogoshi.sico.scheduler.SchedulerService;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of the Container interface.
 * Manages component registration, dependency injection, and lifecycle management.
 * This implementation supports singleton and prototype scopes, package scanning, and component handlers.
 */
public class DefaultContainer implements Container, Lifecycle {
    private static final Logger LOGGER = Logger.getLogger(DefaultContainer.class.getName());
    
    @NotNull private final Map<Class<?>, Object> components = new ConcurrentHashMap<>();
    
    @NotNull private final Map<String, Object> namedComponents = new ConcurrentHashMap<>();
    
    @NotNull private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    
    @NotNull private final Map<Class<?>, Set<String>> typeIndex = new ConcurrentHashMap<>();
    
    @NotNull private final Map<BeanDefinition, Object> prototypeFactories = new ConcurrentHashMap<>();
    
    @NotNull private final Set<Class<?>> registeredClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @NotNull private final Set<Class<?>> processingClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    @NotNull private final List<ComponentRegisterHandler> handlers = new CopyOnWriteArrayList<>();
    @NotNull private final Set<Class<? extends Annotation>> componentAnnotations = new HashSet<>(Arrays.asList(
            Component.class, Service.class, Repository.class, Configuration.class
    ));
    
    @Nullable private PreDestroyHandler preDestroyHandler;
    @Nullable private ScheduledHandler scheduledHandler;
    @Nullable private ConfigurationHandler configurationHandler;
    @NotNull @Getter private final SchedulerService schedulerService;
    private volatile boolean closed = false;
    private volatile boolean running = false;

    /**
     * Creates a new DefaultContainer with a new SchedulerService.
     */
    public DefaultContainer() {
        this(new SchedulerService());
    }
    
    /**
     * Creates a new DefaultContainer with the provided SchedulerService.
     *
     * @param schedulerService the scheduler service to use
     */
    public DefaultContainer(@NotNull SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
        handlers.add(new AutowiredHandler(this));
        handlers.add(new PostConstructHandler(this));
        handlers.add(preDestroyHandler = new PreDestroyHandler(this));
        handlers.add(scheduledHandler = new ScheduledHandler(this, schedulerService));
        handlers.add(configurationHandler = new ConfigurationHandler(this));
    }

    /**
     * Starts the container and its scheduler service.
     */
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

    /**
     * Stops the container and its scheduler service.
     */
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

    /**
     * Checks if the container is running.
     *
     * @return true if the container is running, false otherwise
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Resolves a component by type.
     *
     * @param <T> the type of component to resolve
     * @param clazz the class of the component to resolve
     * @return the component instance, or null if no component of that type exists
     * @throws IllegalStateException if the container is closed
     */
    @Override
    @Nullable
    public <T> T resolve(@NotNull Class<T> clazz) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        // Check if this is a prototype-scoped component
        Scope.Scopes scope = determineComponentScope(clazz);
        if (scope.equals(Scope.Scopes.PROTOTYPE)) {
            // Always create new instances for prototype scope
            if (isComponent(clazz) && !processingClasses.contains(clazz)) {
                try {
                    return clazz.cast(createNewInstance(clazz));
                } catch (Exception e) {
                    throw new RuntimeException("Error creating prototype instance for class: " + clazz.getName(), e);
                }
            }
            return null;
        }
        
        // For singleton scope, check the components map
        Object component = components.get(clazz);
        if (component != null) {
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
    
    /**
     * Resolves a component by name and type.
     *
     * @param <T> the type of component to resolve
     * @param name the name of the component to resolve
     * @param clazz the class of the component to resolve
     * @return the component instance, or null if no component with that name and type exists
     * @throws IllegalStateException if the container is closed
     */
    @Override
    @Nullable
    public <T> T resolve(@NotNull String name, @NotNull Class<T> clazz) {
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

    /**
     * Registers a component class with the container.
     * This creates an instance of the class and processes it with the appropriate handlers.
     *
     * @param clazz the class to register
     * @throws IllegalStateException if the container is closed or if circular dependency is detected
     * @throws RuntimeException if registration fails
     */
    @Override
    public void register(@NotNull Class<?> clazz) {
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
            
            Scope.Scopes scope = determineComponentScope(clazz);
            
            BeanDefinition definition = BeanDefinition.forClass(name, clazz, scope, true);
            registerBeanDefinition(definition);
            
            registerBean(name, instance);
            // Only store singleton-scoped beans in the components map
            if (scope.equals(Scope.Scopes.SINGLETON)) {
                components.put(clazz, instance);
            }
            registeredClasses.add(clazz);
            
            processHandlersForPhase(clazz, Phase.REGISTRATION);
            
        } catch (Exception e) {
            throw new RuntimeException("Error registering class: " + clazz.getName(), e);
        } finally {
            processingClasses.remove(clazz);
        }
    }
    
    /**
     * Registers a bean definition with the container.
     * 
     * @param beanDefinition the bean definition to register
     * @throws IllegalStateException if the container is closed
     */
    @Override
    public void registerBeanDefinition(@NotNull BeanDefinition beanDefinition) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        String name = beanDefinition.getName();
        Class<?> type = beanDefinition.getBeanClass();
        
        beanDefinitions.put(name, beanDefinition);
        
        typeIndex.computeIfAbsent(type, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(name);
        
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
    
    /**
     * Registers a bean instance with the container.
     * 
     * @param name the name of the bean
     * @param instance the bean instance
     * @throws IllegalStateException if the container is closed
     */
    @Override
    public void registerBean(@NotNull String name, @NotNull Object instance) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        namedComponents.put(name, instance);
        
        Class<?> type = instance.getClass();
        typeIndex.computeIfAbsent(type, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(name);
        
        if (!components.containsKey(type)) {
            components.put(type, instance);
        }
    }
    
    /**
     * Creates a bean instance from a bean definition.
     * 
     * @param definition the bean definition
     * @return the bean instance, or null if creation fails
     * @throws BeanCreationException if an error occurs creating the bean
     */
    @Nullable
    private Object createBeanFromDefinition(@NotNull BeanDefinition definition) {
        try {
            if (definition.isFactoryMethod()) {
                Method factoryMethod = definition.getFactoryMethod();
                if (factoryMethod == null) {
                    return null;
                }
                factoryMethod.setAccessible(true);
                
                Object factoryInstance = definition.getDeclaringInstance();
                if (factoryInstance == null) {
                    return null;
                }
                
                Parameter[] parameters = factoryMethod.getParameters();
                Object[] args = new Object[parameters.length];
                
                for (int i = 0; i < parameters.length; i++) {
                    Class<?> paramType = parameters[i].getType();
                    args[i] = resolve(paramType);
                    
                    if (args[i] == null) {
                        throw new BeanCreationException("Failed to resolve dependency of type " + paramType.getName() + 
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
            throw new BeanCreationException("Error creating bean: " + definition.getName(), e);
        }
    }
    
    /**
     * Determines the component name for a class.
     * 
     * @param clazz the class
     * @return the component name
     */
    @NotNull
    private String determineComponentName(@NotNull Class<?> clazz) {
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
        String baseName = Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
        
        // Check if this name already exists and if so, include package to avoid collisions
        if (namedComponents.containsKey(baseName) || beanDefinitions.containsKey(baseName)) {
            String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";
            String[] packageParts = packageName.split("\\.");
            String qualifiedName = packageParts.length > 0 ? 
                packageParts[packageParts.length - 1] + "." + baseName : baseName;
            
            // If still conflicts, use fully qualified name
            if (namedComponents.containsKey(qualifiedName) || beanDefinitions.containsKey(qualifiedName)) {
                return clazz.getName().replace(".", "_").replace("$", "_");
            }
            return qualifiedName;
        }
        
        return baseName;
    }
    
    /**
     * Determines the scope for a component class.
     * 
     * @param clazz the class
     * @return the scope
     */
    @NotNull
    private Scope.Scopes determineComponentScope(@NotNull Class<?> clazz) {
        if (clazz.isAnnotationPresent(Scope.class)) {
            return clazz.getAnnotation(Scope.class).value();
        }
        
        return Scope.Scopes.SINGLETON;
    }
    
    /**
     * Finds a suitable constructor for a class.
     * 
     * @param clazz the class
     * @return the constructor, or null if none is found
     */
    @Nullable
    private Constructor<?> findSuitableConstructor(@NotNull Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(dev.hogoshi.sico.annotation.Autowired.class)) {
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
    
    /**
     * Resolves the parameters for a constructor.
     * 
     * @param constructor the constructor
     * @return the resolved parameters
     * @throws IllegalStateException if a dependency cannot be resolved
     */
    @NotNull
    private Object[] resolveConstructorParameters(@NotNull Constructor<?> constructor) {
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
    
    /**
     * Checks if a class is a component.
     * 
     * @param clazz the class
     * @return true if the class is a component
     */
    private boolean isComponent(@NotNull Class<?> clazz) {
        for (Class<? extends Annotation> annotationType : componentAnnotations) {
            if (clazz.isAnnotationPresent(annotationType)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Processes handlers for a phase for a component class.
     * 
     * @param clazz the class
     * @param phase the phase
     */
    private void processHandlersForPhase(@NotNull Class<?> clazz, @NotNull Phase phase) {
        List<ComponentRegisterHandler> sortedHandlers = new ArrayList<>(handlers);
        Collections.sort(sortedHandlers);
        
        for (ComponentRegisterHandler handler : sortedHandlers) {
            if (handler.getPhase() == phase && handler.supports(clazz)) {
                handler.handle(clazz);
            }
        }
    }

    /**
     * Scans the specified packages for components and registers them in the container.
     * 
     * @param filter a predicate to filter class names during scanning
     * @param packageNames the package names to scan
     */
    @Override
    public void scan(@NotNull Predicate<String> filter, String... packageNames) {
        scan(filter, Thread.currentThread().getContextClassLoader(), packageNames);
    }

    /**
     * Scans the specified packages using a custom class loader and registers the found components in the container.
     * 
     * @param filter a predicate to filter class names during scanning
     * @param classLoader the class loader to use for scanning
     * @param packageNames the package names to scan
     * @throws IllegalStateException if the container is closed
     */
    @Override
    public void scan(@NotNull Predicate<String> filter, @NotNull ClassLoader classLoader, String... packageNames) {
        if (closed) {
            throw new IllegalStateException("Container is closed");
        }
        
        for (String packageName : packageNames) {
            try {
                scanPackage(packageName, filter, classLoader);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error scanning package: " + packageName, e);
            }
        }
        
        Set<Class<?>> classesToProcess = new HashSet<>(registeredClasses);
        for (Class<?> clazz : classesToProcess) {
            processHandlersForPhase(clazz, Phase.POST_PROCESSING);
        }
    }
    
    /**
     * Closes the container and releases all resources.
     */
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
            throw new ContainerException("Error closing container", e);
        }
    }
    
    /**
     * Scans a package for components.
     * 
     * @param packageName the package name
     * @param filter the filter
     * @param classLoader the class loader
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a class cannot be found
     */
    private void scanPackage(@NotNull String packageName, @NotNull Predicate<String> filter, @NotNull ClassLoader classLoader) throws IOException, ClassNotFoundException {
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());
            scanDirectory(directory, packageName, filter, classLoader);
        }
    }
    
    /**
     * Scans a directory for components.
     * 
     * @param directory the directory
     * @param packageName the package name
     * @param filter the filter
     * @param classLoader the class loader
     * @throws ClassNotFoundException if a class cannot be found
     */
    private void scanDirectory(@NotNull File directory, @NotNull String packageName, @NotNull Predicate<String> filter, @NotNull ClassLoader classLoader) throws ClassNotFoundException {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        scanDirectory(file, packageName + "." + file.getName(), filter, classLoader);
                    } else if (file.getName().endsWith(".class")) {
                        String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                        if (filter.test(className)) {
                            try {
                                Class<?> clazz = Class.forName(className, true, classLoader);
                                if (isComponent(clazz)) {
                                    register(clazz);
                                }
                            } catch (Exception e) {
                                throw new ComponentScanException("Error loading class: " + className, e);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Adds a component register handler to the container.
     * 
     * @param handler the handler
     */
    public void addHandler(@NotNull ComponentRegisterHandler handler) {
        handlers.add(handler);
    }
    
    /**
     * Removes a component register handler from the container.
     * 
     * @param handler the handler
     */
    public void removeHandler(@NotNull ComponentRegisterHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Creates a new instance of a class.
     * 
     * @param clazz the class
     * @return the new instance
     * @throws Exception if an error occurs
     */
    @Nullable
    private Object createNewInstance(@NotNull Class<?> clazz) throws Exception {
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
    
    /**
     * Exception thrown when an error occurs creating a bean.
     */
    public static class BeanCreationException extends RuntimeException {
        public BeanCreationException(String message) {
            super(message);
        }
        
        public BeanCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when an error occurs in the container.
     */
    public static class ContainerException extends RuntimeException {
        public ContainerException(String message) {
            super(message);
        }
        
        public ContainerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception thrown when an error occurs during component scanning.
     */
    public static class ComponentScanException extends RuntimeException {
        public ComponentScanException(String message) {
            super(message);
        }
        
        public ComponentScanException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 