package dev.hogoshi.sico;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hogoshi.sico.test.TestComponents.FixedDelayComponent;
import dev.hogoshi.sico.test.TestComponents.FixedRateComponent;

public class ScheduledTest {
    
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
    void testFixedRateScheduling() throws InterruptedException {
        FixedRateComponent component = sico.resolve(FixedRateComponent.class);
        assertNotNull(component, "Component should not be null");
        
        boolean executed = component.getLatch().await(5, TimeUnit.SECONDS);
        assertTrue(executed, "Scheduled method should be executed at least twice");
        
        int count = component.getExecutionCount();
        assertTrue(count >= 2, "Scheduled method should be executed at least twice, got: " + count);
    }
    
    @Test
    void testFixedDelayScheduling() throws InterruptedException {
        FixedDelayComponent component = sico.resolve(FixedDelayComponent.class);
        assertNotNull(component, "Component should not be null");
        
        boolean executed = component.getLatch().await(5, TimeUnit.SECONDS);
        assertTrue(executed, "Scheduled method should be executed at least twice");
        
        int count = component.getExecutionCount();
        assertTrue(count >= 2, "Scheduled method should be executed at least twice, got: " + count);
    }
    
    @Test
    void testSchedulerStopsOnContainerStop() throws InterruptedException {
        FixedRateComponent component = sico.resolve(FixedRateComponent.class);
        assertNotNull(component, "Component should not be null");
        
        Thread.sleep(1000);
        
        sico.stop();
        
        int countAfterStop = component.getExecutionCount();
        
        Thread.sleep(1000);
        
        assertEquals(countAfterStop, component.getExecutionCount(), 
                "Scheduled tasks should not run after container stop");
    }
} 