package dev.hogoshi.sioc.handler;

public interface ComponentRegisterHandler extends Comparable<ComponentRegisterHandler> {

    enum Phase {
        REGISTRATION,
        POST_PROCESSING
    }

    void handle(Class<?> componentClass);

    boolean supports(Class<?> componentClass);

    default int getPriority() {
        return 100;
    }

    default Phase getPhase() {
        return Phase.REGISTRATION;
    }

    @Override
    default int compareTo(ComponentRegisterHandler other) {
        return Integer.compare(this.getPriority(), other.getPriority());
    }
} 