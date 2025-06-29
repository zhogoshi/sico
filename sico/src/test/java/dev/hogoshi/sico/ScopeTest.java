package dev.hogoshi.sioc;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hogoshi.sioc.test.TestComponents.DefaultScopeComponent;
import dev.hogoshi.sioc.test.TestComponents.PrototypeComponent;
import dev.hogoshi.sioc.test.TestComponents.SingletonComponent;

public class ScopeTest {
    
    private Sico sioc;
    
    @BeforeEach
    void setUp() {
        sioc = new Sico();
        sioc.start();

        sioc.scan("dev.hogoshi.sioc.test");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @AfterEach
    void tearDown() {
        sioc.close();
    }
    
    @Test
    void testSingletonScope() {
        SingletonComponent instance1 = sioc.resolve(SingletonComponent.class);
        SingletonComponent instance2 = sioc.resolve(SingletonComponent.class);
        
        assertNotNull(instance1, "First instance should not be null");
        assertNotNull(instance2, "Second instance should not be null");
        assertSame(instance1, instance2, "Singleton components should be the same instance");
    }
    
    @Test
    void testPrototypeScope() {
        PrototypeComponent instance1 = sioc.resolve(PrototypeComponent.class);
        PrototypeComponent instance2 = sioc.resolve(PrototypeComponent.class);
        
        assertNotNull(instance1, "First instance should not be null");
        assertNotNull(instance2, "Second instance should not be null");
        
        assertNotSame(instance1, instance2, "Prototype components should be different instances");
    }
    
    @Test
    void testDefaultScope() {
        DefaultScopeComponent instance1 = sioc.resolve(DefaultScopeComponent.class);
        DefaultScopeComponent instance2 = sioc.resolve(DefaultScopeComponent.class);
        
        assertNotNull(instance1, "First instance should not be null");
        assertNotNull(instance2, "Second instance should not be null");
        assertSame(instance1, instance2, "Default scope should be singleton");
    }
} 