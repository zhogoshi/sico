package dev.hogoshi.sico;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hogoshi.sico.test.TestConfig.ComplexBean;
import dev.hogoshi.sico.test.TestConfig.SimpleBean;

public class ConfigurationTest {
    
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
    void testSingletonBean() {
        SimpleBean bean1 = sico.resolve("simpleBean", SimpleBean.class);
        SimpleBean bean2 = sico.resolve("simpleBean", SimpleBean.class);
        
        assertNotNull(bean1, "Bean should not be null");
        assertNotNull(bean2, "Bean should not be null");
        assertSame(bean1, bean2, "Singleton beans should be the same instance");
        
        assertTrue(bean1.getName().startsWith("singleton-"), 
                "Bean name should start with 'singleton-', got: " + bean1.getName());
    }
    
    @Test
    void testPrototypeBean() {
        SimpleBean bean1 = sico.resolve("prototypeBean", SimpleBean.class);
        SimpleBean bean2 = sico.resolve("prototypeBean", SimpleBean.class);
        
        assertNotNull(bean1, "Bean should not be null");
        assertNotNull(bean2, "Bean should not be null");
        assertNotSame(bean1, bean2, "Prototype beans should be different instances");
        assertEquals("prototype-", bean1.getName().substring(0, 10));
        assertEquals("prototype-", bean2.getName().substring(0, 10));
    }
    
    @Test
    void testNamedBean() {
        SimpleBean bean = sico.resolve("customNamedBean", SimpleBean.class);
        
        assertNotNull(bean, "Bean should not be null");
        assertEquals("named-", bean.getName().substring(0, 6));
    }
    
    @Test
    void testDependencyInjection() {
        ComplexBean complexBean = sico.resolve("complexBean", ComplexBean.class);
        SimpleBean simpleBean = sico.resolve("simpleBean", SimpleBean.class);
        
        assertNotNull(complexBean, "Complex bean should not be null");
        assertNotNull(complexBean.getSimpleBean(), "Injected bean should not be null");
        assertSame(simpleBean, complexBean.getSimpleBean(), "ComplexBean should have the same SimpleBean instance");
    }
} 