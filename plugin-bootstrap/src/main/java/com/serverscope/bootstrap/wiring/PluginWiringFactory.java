package com.serverscope.bootstrap.wiring;

import com.serverscope.analyzer.AnalyzerModule;
import com.serverscope.api.config.ServerScopeConfig;
import com.serverscope.api.lifecycle.ManagedComponent;
import com.serverscope.collectors.CollectorsModule;
import com.serverscope.core.config.DefaultServerScopeConfigProvider;
import com.serverscope.core.i18n.TranslationService;
import com.serverscope.core.lifecycle.DefaultLifecycleManager;
import com.serverscope.core.runtime.DefaultRuntimeInfoService;
import com.serverscope.storage.StorageModule;
import com.serverscope.web.WebModule;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public final class PluginWiringFactory {
    private final JavaPlugin plugin;
    private final Logger logger;

    public PluginWiringFactory(JavaPlugin plugin, Logger logger) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public PluginRuntime create(ServerScopeConfig config) {
        DefaultServerScopeConfigProvider configProvider = new DefaultServerScopeConfigProvider(config);
        DefaultRuntimeInfoService runtimeInfoService = new DefaultRuntimeInfoService();
        TranslationService translations = new TranslationService(logger, config.localization());
        CollectorsModule collectorsModule = new CollectorsModule(plugin, logger, config.collectors(), config.profiler());
        AnalyzerModule analyzerModule = new AnalyzerModule(
                plugin,
                logger,
                config.alerting(),
                translations,
                collectorsModule::latestBatches,
                collectorsModule::profilerSnapshot
        );
        StorageModule storageModule = new StorageModule(
                logger,
                config.storage(),
                collectorsModule::latestBatches,
                collectorsModule::profilerSnapshot,
                analyzerModule::activeAlerts,
                analyzerModule::activeFindings
        );
        WebModule webModule = new WebModule(
                logger,
                config.web(),
                translations,
                collectorsModule::latestBatches,
                collectorsModule::profilerSnapshot,
                analyzerModule::activeAlerts,
                analyzerModule::activeFindings,
                storageModule,
                storageModule
        );

        List<ManagedComponent> components = List.of(
                storageModule,
                collectorsModule,
                analyzerModule,
                webModule
        );

        DefaultLifecycleManager lifecycleManager = new DefaultLifecycleManager(logger, components);
        return new PluginRuntime(
                configProvider,
                runtimeInfoService,
                translations,
                lifecycleManager,
                storageModule,
                collectorsModule,
                analyzerModule,
                webModule
        );
    }
}
