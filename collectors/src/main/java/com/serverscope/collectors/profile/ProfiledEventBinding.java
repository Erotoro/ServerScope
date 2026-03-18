package com.serverscope.collectors.profile;

import org.bukkit.event.Event;

import java.util.Objects;

public record ProfiledEventBinding(
        String id,
        Class<? extends Event> eventClass
) {
    public ProfiledEventBinding {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(eventClass, "eventClass");
    }
}
