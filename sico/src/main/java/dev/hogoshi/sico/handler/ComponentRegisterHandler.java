package dev.hogoshi.sico.handler;

import org.jetbrains.annotations.NotNull;

public interface ComponentRegisterHandler extends Comparable<ComponentRegisterHandler> {

    enum Phase {
        REGISTRATION,
        POST_PROCESSING
    }

    void handle(@NotNull Class<?> componentClass);

    boolean supports(@NotNull Class<?> componentClass);

    default int getPriority() {
        return 100;
    }

    default @NotNull Phase getPhase() {
        return Phase.REGISTRATION;
    }

    @Override
    default int compareTo(@NotNull ComponentRegisterHandler other) {
        return Integer.compare(this.getPriority(), other.getPriority());
    }
} 