package com.serverscope.collectors.server;

import org.bukkit.Server;

import java.lang.reflect.Method;
import java.util.OptionalDouble;
import java.util.OptionalLong;

public final class ServerRuntimeMetricAccess {
    private ServerRuntimeMetricAccess() {
    }

    public static OptionalDouble primaryTps(Server server) {
        return invokeDoubleArrayHead(server, "getTPS");
    }

    public static OptionalDouble averageTickTimeMillis(Server server) {
        return invokeDouble(server, "getAverageTickTime");
    }

    public static OptionalLong totalLoadedChunks(Server server) {
        // O(1) counter path (Paper getChunkCount) is the primary, Folia-safe source.
        // Iterating getLoadedChunks() is only safe on non-Folia main-thread execution.
        return aggregateWorldCounts(server, "getChunkCount", world -> (long) world.getLoadedChunks().length);
    }

    public static OptionalLong loadedChunksForWorld(org.bukkit.World world) {
        return invokeLong(world, "getChunkCount");
    }

    public static OptionalLong totalEntities(Server server) {
        // O(1) counter path (Paper getEntityCount) is the primary, Folia-safe source.
        // Iterating getEntities() across worlds is unsafe under Folia region threading
        // (regions do not share data), so it is only used as a non-Folia fallback.
        return aggregateWorldCounts(server, "getEntityCount", world -> (long) world.getEntities().size());
    }

    /**
     * Sums a per-world quantity across all worlds.
     *
     * <p>The {@code counterMethod} is a Paper O(1) counter read (e.g. {@code getEntityCount}); reading
     * it is safe from any thread because it cannot corrupt region-owned data — at worst it returns a
     * momentarily stale number, which is acceptable for a metric. When the counter is unavailable
     * (legacy Spigot), {@code iterationFallback} is used, but only on non-Folia runtimes, because
     * iterating world entity/chunk collections off the owning region thread is unsafe on Folia.
     *
     * @return the aggregate, or empty if no world could be measured at all
     */
    private static OptionalLong aggregateWorldCounts(
            Server server,
            String counterMethod,
            java.util.function.ToLongFunction<org.bukkit.World> iterationFallback
    ) {
        boolean folia = isFoliaRuntime(server);
        long total = 0L;
        boolean measuredAny = false;
        for (org.bukkit.World world : server.getWorlds()) {
            OptionalLong counter = invokeLong(world, counterMethod);
            if (counter.isPresent()) {
                total += counter.getAsLong();
                measuredAny = true;
                continue;
            }
            if (folia) {
                // No safe way to count this world without the O(1) counter; skip it rather
                // than touching region-owned collections from the global scheduler thread.
                continue;
            }
            total += iterationFallback.applyAsLong(world);
            measuredAny = true;
        }
        return measuredAny ? OptionalLong.of(total) : OptionalLong.empty();
    }

    private static OptionalLong invokeLong(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number number) {
                return OptionalLong.of(number.longValue());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return OptionalLong.empty();
    }

    private static OptionalDouble invokeDoubleArrayHead(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof double[] values && values.length > 0) {
                return OptionalDouble.of(values[0]);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return OptionalDouble.empty();
    }

    private static OptionalDouble invokeDouble(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number number) {
                return OptionalDouble.of(number.doubleValue());
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return OptionalDouble.empty();
    }

    private static boolean isFoliaRuntime(Server server) {
        String serverName = server.getName();
        if (serverName != null && serverName.toLowerCase(java.util.Locale.ROOT).contains("folia")) {
            return true;
        }
        String version = server.getVersion();
        return version != null && version.toLowerCase(java.util.Locale.ROOT).contains("folia");
    }
}
