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
        boolean folia = isFoliaRuntime(server);
        long total = 0L;
        for (org.bukkit.World world : server.getWorlds()) {
            OptionalLong worldCount = loadedChunksForWorld(world);
            if (worldCount.isPresent()) {
                total += worldCount.getAsLong();
                continue;
            }
            if (folia) {
                return OptionalLong.empty();
            }
            total += world.getLoadedChunks().length;
        }
        return OptionalLong.of(total);
    }

    public static OptionalLong loadedChunksForWorld(org.bukkit.World world) {
        return invokeLong(world, "getChunkCount");
    }

    public static OptionalLong totalEntities(Server server) {
        boolean folia = isFoliaRuntime(server);
        long total = 0L;
        for (org.bukkit.World world : server.getWorlds()) {
            OptionalLong worldCount = invokeLong(world, "getEntityCount");
            if (worldCount.isPresent()) {
                total += worldCount.getAsLong();
                continue;
            }
            if (folia) {
                return OptionalLong.empty();
            }
            total += world.getEntities().size();
        }
        return OptionalLong.of(total);
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
