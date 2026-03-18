package com.serverscope.core.lifecycle;

import com.serverscope.api.lifecycle.ManagedComponent;

import java.util.List;

public interface LifecycleManager {
    void startAll();

    void stopAll();

    List<ManagedComponent> components();
}
