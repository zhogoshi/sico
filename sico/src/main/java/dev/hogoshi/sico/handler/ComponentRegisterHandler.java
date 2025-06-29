package dev.hogoshi.sico.handler;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for handlers that process components during registration.
 * Handlers can be registered with the container to provide custom processing
 * logic for components at different phases of the registration process.
 */
public interface ComponentRegisterHandler extends Comparable<ComponentRegisterHandler> {

    /**
     * Handles the component class.
     * 
     * @param componentClass the component class to handle
     */
    void handle(@NotNull Class<?> componentClass);

    /**
     * Checks if this handler supports the given component class.
     * 
     * @param componentClass the component class to check
     * @return true if this handler supports the component class, false otherwise
     */
    boolean supports(@NotNull Class<?> componentClass);

    /**
     * Gets the phase in which this handler should be executed.
     * 
     * @return the phase
     */
    @NotNull
    Phase getPhase();

    /**
     * Gets the order of this handler within its phase.
     * Lower values indicate higher priority.
     * 
     * @return the order
     */
    int getOrder();
    
    /**
     * Compares this handler with another handler based on phase and order.
     * 
     * @param other the other handler
     * @return a negative integer, zero, or a positive integer as this handler has higher priority,
     *         equal priority, or lower priority than the other handler
     */
    @Override
    default int compareTo(@NotNull ComponentRegisterHandler other) {
        if (getPhase() != other.getPhase()) {
            return getPhase().ordinal() - other.getPhase().ordinal();
        }
        return getOrder() - other.getOrder();
    }
    
    /**
     * The phase in which a handler is executed.
     */
    enum Phase {
        /**
         * The registration phase, where components are initially registered.
         */
        REGISTRATION,
        
        /**
         * The post-processing phase, where components are processed after all components have been registered.
         */
        POST_PROCESSING
    }
} 