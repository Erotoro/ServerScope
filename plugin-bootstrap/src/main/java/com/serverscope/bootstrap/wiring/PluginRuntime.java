package com.serverscope.bootstrap.wiring;

import com.serverscope.analyzer.AnalyzerModule;
import com.serverscope.collectors.CollectorsModule;
import com.serverscope.storage.StorageModule;
import com.serverscope.web.WebModule;
import com.serverscope.api.config.ServerScopeConfigProvider;
import com.serverscope.api.service.RuntimeInfoService;
import com.serverscope.core.i18n.TranslationService;
import com.serverscope.core.lifecycle.LifecycleManager;

import java.util.Objects;

public record PluginRuntime(
        ServerScopeConfigProvider configProvider,
        RuntimeInfoService runtimeInfoService,
        TranslationService translations,
        LifecycleManager lifecycleManager,
        StorageModule storageModule,
        CollectorsModule collectorsModule,
        AnalyzerModule analyzerModule,
        WebModule webModule
) {
    public PluginRuntime {
        Objects.requireNonNull(configProvider, "configProvider");
        Objects.requireNonNull(runtimeInfoService, "runtimeInfoService");
        Objects.requireNonNull(translations, "translations");
        Objects.requireNonNull(lifecycleManager, "lifecycleManager");
        Objects.requireNonNull(storageModule, "storageModule");
        Objects.requireNonNull(collectorsModule, "collectorsModule");
        Objects.requireNonNull(analyzerModule, "analyzerModule");
        Objects.requireNonNull(webModule, "webModule");
    }
}
