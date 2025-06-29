package dev.hogoshi.sico.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import dev.hogoshi.sico.annotation.Autowired;
import dev.hogoshi.sico.annotation.Component;
import dev.hogoshi.sico.annotation.PostConstruct;
import dev.hogoshi.sico.annotation.PreDestroy;
import dev.hogoshi.sico.annotation.Scheduled;
import dev.hogoshi.sico.annotation.Scope;
import dev.hogoshi.sico.annotation.Service;

public class TestComponents {
    
    
    @Service
    public static class TestService {
        public String sayHello() {
            return "Hello from TestService";
        }
    }
    
    @Component
    public static class TestComponent {
        @Autowired
        private TestService service;
        
        public TestService getService() {
            return service;
        }
    }
    
    @Component
    public static class ConstructorInjectedComponent {
        private final TestService service;
        
        public ConstructorInjectedComponent(TestService service) {
            this.service = service;
        }
        
        public TestService getService() {
            return service;
        }
    }
    
    @Component
    public static class CircularA {
        @Autowired
        private CircularB circularB;
    }
    
    @Component
    public static class CircularB {
        @Autowired
        private CircularA circularA;
    }
    
    
    @Component
    public static class LifecycleComponent {
        private boolean initialized = false;
        private boolean destroyed = false;
        
        @PostConstruct
        public void init() {
            initialized = true;
        }
        
        @PreDestroy
        public void cleanup() {
            destroyed = true;
        }
        
        public boolean isInitialized() {
            return initialized;
        }
        
        public boolean isDestroyed() {
            return destroyed;
        }
    }
    
    @Component
    public static class OrderedLifecycleComponent {
        private final List<String> events = new ArrayList<>();
        
        @PostConstruct
        public void init() {
            events.add("init");
        }
        
        @PreDestroy
        public void cleanup() {
            events.add("destroy");
        }
        
        public List<String> getEvents() {
            return events;
        }
    }
    
    
    @Component
    public static class FixedRateComponent {
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private final CountDownLatch latch = new CountDownLatch(2);
        
        @Scheduled(interval = 200, unit = TimeUnit.MILLISECONDS, fixedRate = true)
        public void scheduledTask() {
            executionCount.incrementAndGet();
            latch.countDown();
        }
        
        public int getExecutionCount() {
            return executionCount.get();
        }
        
        public CountDownLatch getLatch() {
            return latch;
        }
    }
    
    @Component
    public static class FixedDelayComponent {
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private final CountDownLatch latch = new CountDownLatch(2);
        
        @Scheduled(interval = 200, unit = TimeUnit.MILLISECONDS, fixedRate = false)
        public void scheduledTask() {
            executionCount.incrementAndGet();
            latch.countDown();
        }
        
        public int getExecutionCount() {
            return executionCount.get();
        }
        
        public CountDownLatch getLatch() {
            return latch;
        }
    }
    
    
    @Component
    @Scope(Scope.Scopes.SINGLETON)
    public static class SingletonComponent {
        public SingletonComponent() {
        }
    }
    
    @Component
    @Scope(Scope.Scopes.PROTOTYPE)
    public static class PrototypeComponent {
        public PrototypeComponent() {
        }
    }
    
    @Component
    public static class DefaultScopeComponent {
        public DefaultScopeComponent() {
        }
    }
} 