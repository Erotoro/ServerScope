package com.serverscope.api.lifecycle;

public interface ManagedComponent extends PluginComponent {
    String name();

    ComponentHealth health();

    default boolean toleratesDegradedStartup() {
        return false;
    }
}
