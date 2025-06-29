package dev.hogoshi.sioc.container;

import java.lang.reflect.Method;

public class BeanDefinition {
    private final String name;
    private final Class<?> beanClass;
    private final String scope;
    private final boolean autowireCandidate;
    
    private Object declaringInstance;
    private Method factoryMethod;
    
    private BeanDefinition(String name, Class<?> beanClass, String scope, boolean autowireCandidate) {
        this.name = name;
        this.beanClass = beanClass;
        this.scope = scope;
        this.autowireCandidate = autowireCandidate;
    }
    
    private BeanDefinition(String name, Class<?> beanClass, String scope, boolean autowireCandidate, 
                          Object declaringInstance, Method factoryMethod) {
        this.name = name;
        this.beanClass = beanClass;
        this.scope = scope;
        this.autowireCandidate = autowireCandidate;
        this.declaringInstance = declaringInstance;
        this.factoryMethod = factoryMethod;
    }

    public static BeanDefinition forClass(String name, Class<?> beanClass, String scope, boolean autowireCandidate) {
        return new BeanDefinition(name, beanClass, scope, autowireCandidate);
    }

    public static BeanDefinition forMethod(String name, Class<?> beanClass, String scope, boolean autowireCandidate,
                                          Object declaringInstance, Method factoryMethod) {
        return new BeanDefinition(name, beanClass, scope, autowireCandidate, declaringInstance, factoryMethod);
    }

    public String getName() {
        return name;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public String getScope() {
        return scope;
    }

    public boolean isSingleton() {
        return scope.equals("singleton");
    }

    public boolean isPrototype() {
        return scope.equals("prototype");
    }

    public boolean isAutowireCandidate() {
        return autowireCandidate;
    }

    public boolean isFactoryMethod() {
        return factoryMethod != null;
    }

    public Object getDeclaringInstance() {
        return declaringInstance;
    }

    public Method getFactoryMethod() {
        return factoryMethod;
    }
    
    @Override
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