package com.serverscope.core.concurrent;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Java 17-compatible factory for named platform threads. Replaces the Java 21
 * {@code Thread.ofPlatform()} builder so a single build can target older runtimes
 * (Minecraft 1.18+) while keeping identical thread naming and daemon semantics.
 */
public final class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final boolean daemon;
    private final boolean numbered;
    private final AtomicLong counter = new AtomicLong();

    private NamedThreadFactory(String prefix, boolean daemon, boolean numbered) {
        this.prefix = Objects.requireNonNull(prefix, "prefix");
        this.daemon = daemon;
        this.numbered = numbered;
    }

    /** Factory for daemon threads that all share the exact same name (single-thread executors). */
    public static NamedThreadFactory daemon(String prefix) {
        return new NamedThreadFactory(prefix, true, false);
    }

    /** Factory for daemon threads with a stable {@code prefix + index} name (thread pools). */
    public static NamedThreadFactory daemonNumbered(String prefix) {
        return new NamedThreadFactory(prefix, true, true);
    }

    /** Creates a single non-daemon worker thread (not started). */
    public static Thread newWorkerThread(String name, Runnable runnable) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(false);
        return thread;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = numbered ? prefix + counter.getAndIncrement() : prefix;
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(daemon);
        return thread;
    }
}
