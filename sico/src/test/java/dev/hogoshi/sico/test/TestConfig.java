package dev.hogoshi.sioc.test;

import java.util.concurrent.atomic.AtomicInteger;

import dev.hogoshi.sioc.annotation.Bean;
import dev.hogoshi.sioc.annotation.Configuration;
import dev.hogoshi.sioc.annotation.Scope;

@Configuration
public class TestConfig {
    
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Bean
    public SimpleBean simpleBean() {
        int count = counter.incrementAndGet();
        return new SimpleBean("singleton-" + count);
    }
    
    @Bean
    @Scope(Scope.Scopes.PROTOTYPE)
    public SimpleBean prototypeBean() {
        int count = counter.incrementAndGet();
        return new SimpleBean("prototype-" + count);
    }
    
    @Bean(name = "customNamedBean")
    public SimpleBean namedBean() {
        int count = counter.incrementAndGet();
        return new SimpleBean("named-" + count);
    }
    
    @Bean
    public ComplexBean complexBean(SimpleBean simpleBean) {
        return new ComplexBean(simpleBean);
    }
    
    public static class SimpleBean {
        private final String name;
        
        public SimpleBean(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }
    
    public static class ComplexBean {
        private final SimpleBean simpleBean;
        
        public ComplexBean(SimpleBean simpleBean) {
            this.simpleBean = simpleBean;
        }
        
        public SimpleBean getSimpleBean() {
            return simpleBean;
        }
    }
} 