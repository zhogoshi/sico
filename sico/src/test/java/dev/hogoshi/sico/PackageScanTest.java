package dev.hogoshi.sioc;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hogoshi.sioc.test.TestComponents.LifecycleComponent;
import dev.hogoshi.sioc.test.TestComponents.TestComponent;
import dev.hogoshi.sioc.test.TestComponents.TestService;
import dev.hogoshi.sioc.test.TestConfig.SimpleBean;

public class PackageScanTest {
    
    private Sico sioc;
    
    @BeforeEach
    void setUp() {
        sioc = new Sico();
        sioc.start();
    }
    
    @AfterEach
    void tearDown() {
        sioc.close();
    }
    
    @Test
    void testPackageScan() {
        sioc.scan("dev.hogoshi.sioc.test");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        TestService service = sioc.resolve(TestService.class);
        assertNotNull(service, "TestService should be found");
        
        TestComponent component = sioc.resolve(TestComponent.class);
        assertNotNull(component, "TestComponent should be found");
        assertNotNull(component.getService(), "TestComponent should have autowired service");
        
        LifecycleComponent lifecycleComponent = sioc.resolve(LifecycleComponent.class);
        assertNotNull(lifecycleComponent, "LifecycleComponent should be found");
        assertTrue(lifecycleComponent.isInitialized(), "PostConstruct should be called");
        
        SimpleBean simpleBean = sioc.resolve("simpleBean", SimpleBean.class);
        assertNotNull(simpleBean, "SimpleBean should be found");
        
        assertFalse(lifecycleComponent.isDestroyed(), "PreDestroy should not be called yet");
    }
} 