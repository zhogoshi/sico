package dev.hogoshi.sico;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hogoshi.sico.test.TestComponents.LifecycleComponent;
import dev.hogoshi.sico.test.TestComponents.OrderedLifecycleComponent;

public class LifecycleTest {
    
    private Sico sico;
    
    @BeforeEach
    void setUp() {
        sico = new Sico();
        sico.start();
        
        sico.scan("dev.hogoshi.sico.test");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @AfterEach
    void tearDown() {
        sico.close();
    }
    
    @Test
    void testPostConstructCalled() {
        LifecycleComponent component = sico.resolve(LifecycleComponent.class);
        assertNotNull(component, "Component should not be null");
        assertTrue(component.isInitialized(), "PostConstruct method should be called");
    }
    
    @Test
    void testPreDestroyCalled() {
        LifecycleComponent component = sico.resolve(LifecycleComponent.class);
        assertNotNull(component, "Component should not be null");
        assertFalse(component.isDestroyed(), "PreDestroy method should not be called yet");
        
        sico.close();
        
        assertTrue(component.isDestroyed(), "PreDestroy method should be called after container close");
    }
    
    @Test
    void testLifecycleOrder() {
        OrderedLifecycleComponent component = sico.resolve(OrderedLifecycleComponent.class);
        assertNotNull(component, "Component should not be null");
        
        List<String> events = component.getEvents();
        assertEquals(1, events.size(), "Only PostConstruct should be called");
        assertEquals("init", events.get(0), "First event should be init");
        
        sico.close();
        
        events = component.getEvents();
        assertEquals(2, events.size(), "Both PostConstruct and PreDestroy should be called");
        assertEquals("init", events.get(0), "First event should be init");
        assertEquals("destroy", events.get(1), "Second event should be destroy");
    }
} 