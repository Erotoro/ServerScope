package com.serverscope.api.config;

import java.util.Objects;

public record CollectorsConfig(
        boolean enabled,
        ServerCollectorsConfig server,
        WorldCollectorsConfig world
) {
    public CollectorsConfig {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(world, "world");
    }
}
