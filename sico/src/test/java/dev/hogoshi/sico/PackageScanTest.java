package dev.hogoshi.sico;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hogoshi.sico.test.TestComponents.LifecycleComponent;
import dev.hogoshi.sico.test.TestComponents.TestComponent;
import dev.hogoshi.sico.test.TestComponents.TestService;
import dev.hogoshi.sico.test.TestConfig.SimpleBean;

public class PackageScanTest {
    
    private Sico sico;
    
    @BeforeEach
    void setUp() {
        sico = new Sico();
        sico.start();
    }
    
    @AfterEach
    void tearDown() {
        sico.close();
    }
    
    @Test
    void testPackageScan() {
        sico.scan("dev.hogoshi.sico.test");
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        TestService service = sico.resolve(TestService.class);
        assertNotNull(service, "TestService should be found");
        
        TestComponent component = sico.resolve(TestComponent.class);
        assertNotNull(component, "TestComponent should be found");
        assertNotNull(component.getService(), "TestComponent should have autowired service");
        
        LifecycleComponent lifecycleComponent = sico.resolve(LifecycleComponent.class);
        assertNotNull(lifecycleComponent, "LifecycleComponent should be found");
        assertTrue(lifecycleComponent.isInitialized(), "PostConstruct should be called");
        
        SimpleBean simpleBean = sico.resolve("simpleBean", SimpleBean.class);
        assertNotNull(simpleBean, "SimpleBean should be found");
        
        assertFalse(lifecycleComponent.isDestroyed(), "PreDestroy should not be called yet");
    }
} 