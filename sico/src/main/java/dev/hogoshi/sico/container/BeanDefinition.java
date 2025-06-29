package dev.hogoshi.sico.container;

import java.lang.reflect.Method;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.hogoshi.sico.annotation.Scope;
import lombok.Getter;

/**
 * Represents a bean definition in the container.
 * A bean definition contains metadata about a bean, such as its name, class, scope, and whether it's a candidate for autowiring.
 * For factory method beans, it also contains the declaring instance and factory method.
 */
@Getter
public class BeanDefinition {
    @NotNull private final String name;
    @NotNull private final Class<?> beanClass;
    @NotNull private final Scope.Scopes scope;
    private final boolean autowireCandidate;
    
    @Nullable private Object declaringInstance;
    @Nullable private Method factoryMethod;
    
    /**
     * Creates a new bean definition for a class.
     *
     * @param name the name of the bean
     * @param beanClass the class of the bean
     * @param scope the scope of the bean
     * @param autowireCandidate whether the bean is a candidate for autowiring
     */
    private BeanDefinition(@NotNull String name, @NotNull Class<?> beanClass, @NotNull Scope.Scopes scope, boolean autowireCandidate) {
        this.name = name;
        this.beanClass = beanClass;
        this.scope = scope;
        this.autowireCandidate = autowireCandidate;
    }
    
    /**
     * Creates a new bean definition for a factory method.
     *
     * @param name the name of the bean
     * @param beanClass the class of the bean
     * @param scope the scope of the bean
     * @param autowireCandidate whether the bean is a candidate for autowiring
     * @param declaringInstance the instance that declares the factory method
     * @param factoryMethod the factory method
     */
    private BeanDefinition(@NotNull String name, @NotNull Class<?> beanClass, @NotNull Scope.Scopes scope, boolean autowireCandidate,
                           @Nullable Object declaringInstance, @Nullable Method factoryMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.scope = scope;
        this.autowireCandidate = autowireCandidate;
        this.declaringInstance = declaringInstance;
        this.factoryMethod = factoryMethod;
    }

    /**
     * Creates a new bean definition for a class.
     *
     * @param name the name of the bean
     * @param beanClass the class of the bean
     * @param scope the scope of the bean
     * @param autowireCandidate whether the bean is a candidate for autowiring
     * @return the bean definition
     */
    @NotNull
    public static BeanDefinition forClass(@NotNull String name, @NotNull Class<?> beanClass, @NotNull Scope.Scopes scope, boolean autowireCandidate) {
        return new BeanDefinition(name, beanClass, scope, autowireCandidate);
    }

    /**
     * Creates a new bean definition for a factory method.
     *
     * @param name the name of the bean
     * @param beanClass the class of the bean
     * @param scope the scope of the bean
     * @param autowireCandidate whether the bean is a candidate for autowiring
     * @param declaringInstance the instance that declares the factory method
     * @param factoryMethod the factory method
     * @return the bean definition
     */
    @NotNull
    public static BeanDefinition forMethod(@NotNull String name, @NotNull Class<?> beanClass, @NotNull Scope.Scopes scope, boolean autowireCandidate,
                                           @NotNull Object declaringInstance, @NotNull Method factoryMethod) {
        return new BeanDefinition(name, beanClass, scope, autowireCandidate, declaringInstance, factoryMethod);
    }

    /**
     * Checks if the bean is a singleton.
     *
     * @return true if the bean is a singleton
     */
    public boolean isSingleton() {
        return scope.equals(Scope.Scopes.SINGLETON);
    }

    /**
     * Checks if the bean is a prototype.
     *
     * @return true if the bean is a prototype
     */
    public boolean isPrototype() {
        return scope.equals(Scope.Scopes.PROTOTYPE);
    }

    /**
     * Checks if the bean is created using a factory method.
     *
     * @return true if the bean is created using a factory method
     */
    public boolean isFactoryMethod() {
        return factoryMethod != null;
    }

    /**
     * Returns a string representation of the bean definition.
     *
     * @return a string representation of the bean definition
     */
    @Override
    @NotNull
    public String toString() {
        StringBuilder sb = new StringBuilder("BeanDefinition{");
        sb.append("name='").append(name).append('\'');
        sb.append(", beanClass=").append(beanClass.getName());
        sb.append(", scope='").append(scope).append('\'');
        sb.append(", autowireCandidate=").append(autowireCandidate);
        
        if (isFactoryMethod()) {
            sb.append(", factoryMethod=").append(factoryMethod.getName());
            sb.append(", declaringClass=").append(factoryMethod.getDeclaringClass().getName());
        }
        
        sb.append('}');
        return sb.toString();
    }
} 