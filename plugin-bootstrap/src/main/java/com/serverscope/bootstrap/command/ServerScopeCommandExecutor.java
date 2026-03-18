package com.serverscope.bootstrap.command;

import com.serverscope.api.alert.AlertRecord;
import com.serverscope.api.diagnostic.DiagnosticFinding;
import com.serverscope.api.diagnostic.DiagnosticReference;
import com.serverscope.api.diagnostic.FindingSeverity;
import com.serverscope.api.metric.MetricBatch;
import com.serverscope.api.metric.MetricSample;
import com.serverscope.bootstrap.ServerScopePlugin;
import com.serverscope.bootstrap.wiring.PluginRuntime;
import com.serverscope.core.i18n.TranslationService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ServerScopeCommandExecutor implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "serverscope.admin";
    private static final String STATUS_PERMISSION = "serverscope.command.status";
    private static final String RELOAD_PERMISSION = "serverscope.command.reload";
    private static final String REPORT_PERMISSION = "serverscope.command.report";
    private static final String FINDINGS_PERMISSION = "serverscope.command.findings";
    private static final String ALERTS_PERMISSION = "serverscope.command.alerts";
    private static final String WEB_PERMISSION = "serverscope.command.web";
    private static final int MAX_LIST_ENTRIES = 5;

    private final ServerScopePlugin plugin;

    public ServerScopeCommandExecutor(ServerScopePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(error(t("command.no_permission")));
            return true;
        }

        if (args.length == 0) {
            sendRootSummary(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status" -> handleStatus(sender);
            case "reload" -> handleReload(sender);
            case "report" -> handleReport(sender);
            case "findings" -> handleFindings(sender);
            case "alerts" -> handleAlerts(sender);
            case "web" -> handleWeb(sender, args);
            default -> {
                sendRootSummary(sender, label);
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return availableCommands(sender).stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }
        if (args.length == 2 && "web".equalsIgnoreCase(args[0]) && sender.hasPermission(WEB_PERMISSION)) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("regenerate-token").stream()
                    .filter(value -> value.startsWith(prefix))
                    .toList();
        }
        return List.of();
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission(STATUS_PERMISSION)) {
            sender.sendMessage(missingPermission(STATUS_PERMISSION));
            return true;
        }

        PluginRuntime runtime = plugin.requireRuntime();
        int alertCount = runtime.analyzerModule().activeAlerts().size();
        int findingCount = runtime.analyzerModule().activeFindings().size();

        sender.sendMessage(header(t("command.header.status")));
        sender.sendMessage(line(t("command.label.runtime"), runtime.runtimeInfoService().isStarted() ? t("command.value.started") : t("command.value.stopped"),
                runtime.runtimeInfoService().isStarted() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(line(t("command.label.storage"), runtime.storageModule().health().status().name(), statusColor(runtime.storageModule().health().status().name())));
        sender.sendMessage(line(t("command.label.collectors"), t("command.collectors.summary", Map.of(
                "status", runtime.collectorsModule().health().status().name(),
                "count", runtime.collectorsModule().registeredCollectorIds().size()
        )), statusColor(runtime.collectorsModule().health().status().name())));
        sender.sendMessage(line(t("command.label.analyzer"), t("command.analyzer.summary", Map.of(
                "status", runtime.analyzerModule().health().status().name(),
                "findings", findingCount,
                "alerts", alertCount
        )), statusColor(runtime.analyzerModule().health().status().name())));
        sender.sendMessage(line(t("command.label.web"), runtime.webModule().health().status().name(), statusColor(runtime.webModule().health().status().name())));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendMessage(missingPermission(RELOAD_PERMISSION));
            return true;
        }

        sender.sendMessage(info(t("command.reload.starting")));
        plugin.reloadServerScopeRuntime(sender);
        return true;
    }

    private boolean handleReport(CommandSender sender) {
        if (!sender.hasPermission(REPORT_PERMISSION)) {
            sender.sendMessage(missingPermission(REPORT_PERMISSION));
            return true;
        }

        PluginRuntime runtime = plugin.requireRuntime();
        Map<String, MetricBatch> batches = runtime.collectorsModule().latestBatches();
        Double tps = latestMetricValue(batches, "SERVER_TPS");
        Double mspt = latestMetricValue(batches, "SERVER_MSPT");
        Double players = latestMetricValue(batches, "PLAYERS_ONLINE");
        Double entities = latestMetricValue(batches, "ENTITY_COUNT");
        List<AlertRecord> alerts = runtime.analyzerModule().activeAlerts();
        List<DiagnosticFinding> findings = runtime.analyzerModule().activeFindings();
        var profiler = runtime.collectorsModule().profilerSnapshot();
        DiagnosticFinding topFinding = findings.stream()
                .sorted(Comparator.comparing(DiagnosticFinding::severity, this::compareSeverity).reversed()
                        .thenComparing(DiagnosticFinding::timestamp).reversed())
                .findFirst()
                .orElse(null);

        sender.sendMessage(header(t("command.header.report")));
        sender.sendMessage(line(t("command.label.tps_mspt"), formatDouble(tps) + " / " + formatDouble(mspt), metricColor(tps, mspt)));
        sender.sendMessage(line(t("command.label.players"), formatDouble(players), NamedTextColor.AQUA));
        sender.sendMessage(line(t("command.label.entities"), formatDouble(entities), NamedTextColor.AQUA));
        sender.sendMessage(line(t("command.label.active_alerts"), Integer.toString(alerts.size()), alerts.isEmpty() ? NamedTextColor.GREEN : NamedTextColor.GOLD));
        sender.sendMessage(line(t("command.label.active_findings"), Integer.toString(findings.size()), findings.isEmpty() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));

        if (!alerts.isEmpty()) {
            AlertRecord topAlert = alerts.getFirst();
            sender.sendMessage(line(t("command.label.top_alert"), topAlert.code() + " - " + topAlert.message(), severityColor(topAlert.severity().name())));
        }

        if (topFinding != null) {
            sender.sendMessage(line(t("command.label.top_finding"), topFinding.title(), findingColor(topFinding.severity())));
        }
        if (profiler != null && !profiler.topSlowEvents().isEmpty()) {
            var topEvent = profiler.topSlowEvents().getFirst();
            sender.sendMessage(line(t("command.label.slowest_event"), t("command.value.slowest_event", Map.of(
                            "eventId", topEvent.eventId(),
                            "avgMs", nanosToMillis(topEvent.averageTimeNanos())
                    )),
                    NamedTextColor.GOLD));
        }

        if (profiler != null && !profiler.topPlugins().isEmpty()) {
            var topPlugin = profiler.topPlugins().getFirst();
            sender.sendMessage(line(t("command.label.heaviest_plugin"), t("command.value.heaviest_plugin", Map.of(
                    "plugin", topPlugin.pluginName(),
                    "totalMs", nanosToMillis(topPlugin.attributedTotalTimeNanos())
            )), NamedTextColor.GOLD));
        }
        return true;
    }

    private boolean handleFindings(CommandSender sender) {
        if (!sender.hasPermission(FINDINGS_PERMISSION)) {
            sender.sendMessage(missingPermission(FINDINGS_PERMISSION));
            return true;
        }

        List<DiagnosticFinding> findings = new ArrayList<>(plugin.requireRuntime().analyzerModule().activeFindings());
        findings.sort(Comparator.comparing(DiagnosticFinding::severity, this::compareSeverity).reversed()
                .thenComparing(DiagnosticFinding::timestamp).reversed());

        sender.sendMessage(header(t("command.header.findings")));
        if (findings.isEmpty()) {
            sender.sendMessage(success(t("command.findings.empty")));
            return true;
        }

        for (int i = 0; i < Math.min(MAX_LIST_ENTRIES, findings.size()); i++) {
            DiagnosticFinding finding = findings.get(i);
            sender.sendMessage(formatFindingSummary(finding));
            sender.sendMessage(secondary("  " + t("command.label.cause") + ": " + finding.probableCause()));
            sender.sendMessage(secondary("  " + t("command.label.action") + ": " + finding.suggestedAction()));
        }
        if (findings.size() > MAX_LIST_ENTRIES) {
            sender.sendMessage(muted(t("command.list.showing", Map.of("limit", MAX_LIST_ENTRIES, "total", findings.size()))));
        }
        return true;
    }

    private boolean handleAlerts(CommandSender sender) {
        if (!sender.hasPermission(ALERTS_PERMISSION)) {
            sender.sendMessage(missingPermission(ALERTS_PERMISSION));
            return true;
        }

        PluginRuntime runtime = plugin.requireRuntime();
        List<AlertRecord> alerts = new ArrayList<>(runtime.analyzerModule().activeAlerts());
        alerts.sort(Comparator.comparing(AlertRecord::occurredAt).reversed());

        sender.sendMessage(header(t("command.header.alerts")));
        if (alerts.isEmpty()) {
            sender.sendMessage(success(t("command.alerts.empty")));
            return true;
        }

        for (int i = 0; i < Math.min(MAX_LIST_ENTRIES, alerts.size()); i++) {
            AlertRecord alert = alerts.get(i);
            sender.sendMessage(formatAlertSummary(alert));
        }
        if (alerts.size() > MAX_LIST_ENTRIES) {
            sender.sendMessage(muted(t("command.list.showing", Map.of("limit", MAX_LIST_ENTRIES, "total", alerts.size()))));
        }
        return true;
    }

    private boolean handleWeb(CommandSender sender, String[] args) {
        if (!sender.hasPermission(WEB_PERMISSION)) {
            sender.sendMessage(missingPermission(WEB_PERMISSION));
            return true;
        }
        if (args.length < 2 || !"regenerate-token".equalsIgnoreCase(args[1])) {
            sender.sendMessage(info(t("command.web.usage")));
            return true;
        }
        sender.sendMessage(info(t("command.web.regenerate.starting")));
        plugin.regenerateWebToken(sender);
        return true;
    }

    private void sendRootSummary(CommandSender sender, String label) {
        PluginRuntime runtime = plugin.requireRuntime();
        Double tps = latestMetricValue(runtime.collectorsModule().latestBatches(), "SERVER_TPS");
        Double mspt = latestMetricValue(runtime.collectorsModule().latestBatches(), "SERVER_MSPT");

        sender.sendMessage(header(t("command.header.root")));
        sender.sendMessage(line(t("command.label.health"), t("command.value.health", Map.of(
                "tps", formatDouble(tps),
                "mspt", formatDouble(mspt)
        )), metricColor(tps, mspt)));
        sender.sendMessage(line(t("command.label.problems"), t("command.value.problems", Map.of(
                "findings", runtime.analyzerModule().activeFindings().size(),
                "alerts", runtime.analyzerModule().activeAlerts().size()
        )), NamedTextColor.YELLOW));
        sender.sendMessage(muted(t("command.value.usage", Map.of("label", label))));
    }

    private List<String> availableCommands(CommandSender sender) {
        List<String> commands = new ArrayList<>();
        if (sender.hasPermission(STATUS_PERMISSION)) {
            commands.add("status");
        }
        if (sender.hasPermission(RELOAD_PERMISSION)) {
            commands.add("reload");
        }
        if (sender.hasPermission(REPORT_PERMISSION)) {
            commands.add("report");
        }
        if (sender.hasPermission(FINDINGS_PERMISSION)) {
            commands.add("findings");
        }
        if (sender.hasPermission(ALERTS_PERMISSION)) {
            commands.add("alerts");
        }
        if (sender.hasPermission(WEB_PERMISSION)) {
            commands.add("web");
        }
        return commands;
    }

    private String formatDouble(Double value) {
        return value == null ? t("command.value.na") : "%.2f".formatted(value);
    }

    private String nanosToMillis(long nanos) {
        return "%.2f".formatted(nanos / 1_000_000.0d);
    }

    private Double latestMetricValue(Map<String, MetricBatch> batches, String metricType) {
        return batches.values().stream()
                .flatMap(batch -> batch.samples().stream())
                .filter(sample -> sample.type().name().equals(metricType))
                .filter(sample -> sample.labels().isEmpty())
                .max(Comparator.comparing(MetricSample::timestamp))
                .map(MetricSample::numericValue)
                .orElse(null);
    }

    private Component formatAlertSummary(AlertRecord alert) {
        return Component.text()
                .append(Component.text("[" + t("enum.severity." + alert.severity().name().toLowerCase(Locale.ROOT)) + "] ", severityColor(alert.severity().name())))
                .append(Component.text(alert.code(), NamedTextColor.YELLOW))
                .append(Component.text(" - " + alert.message(), NamedTextColor.GRAY))
                .build();
    }

    private Component formatFindingSummary(DiagnosticFinding finding) {
        TextComponent.Builder builder = Component.text()
                .append(Component.text("[" + t("enum.severity." + finding.severity().name().toLowerCase(Locale.ROOT)) + "] ", findingColor(finding.severity())))
                .append(Component.text(finding.title(), NamedTextColor.YELLOW))
                .append(Component.text(" - " + finding.description(), NamedTextColor.GRAY));

        String reference = formatReference(finding.reference());
        if (!reference.isBlank()) {
            builder.append(Component.text(" (" + reference + ")", NamedTextColor.DARK_GRAY));
        }
        return builder.build();
    }

    private String formatReference(DiagnosticReference reference) {
        if (reference == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (reference.worldName() != null && !reference.worldName().isBlank()) {
            parts.add("world=" + reference.worldName());
        }
        if (reference.chunkX() != null && reference.chunkZ() != null) {
            parts.add("chunk=" + reference.chunkX() + "," + reference.chunkZ());
        }
        if (reference.pluginName() != null && !reference.pluginName().isBlank()) {
            parts.add("plugin=" + reference.pluginName());
        }
        return String.join(", ", parts);
    }

    private int compareSeverity(FindingSeverity left, FindingSeverity right) {
        return Integer.compare(severityRank(left), severityRank(right));
    }

    private int severityRank(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 3;
            case WARN -> 2;
            case INFO -> 1;
        };
    }

    private NamedTextColor metricColor(Double tps, Double mspt) {
        if (tps == null || mspt == null) {
            return NamedTextColor.GRAY;
        }
        if (tps < 18.0d || mspt > 55.0d) {
            return NamedTextColor.RED;
        }
        if (tps < 19.0d || mspt > 40.0d) {
            return NamedTextColor.GOLD;
        }
        return NamedTextColor.GREEN;
    }

    private NamedTextColor severityColor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> NamedTextColor.RED;
            case "WARN" -> NamedTextColor.GOLD;
            default -> NamedTextColor.GREEN;
        };
    }

    private NamedTextColor findingColor(FindingSeverity severity) {
        return switch (severity) {
            case CRITICAL -> NamedTextColor.RED;
            case WARN -> NamedTextColor.GOLD;
            case INFO -> NamedTextColor.GREEN;
        };
    }

    private NamedTextColor statusColor(String status) {
        return switch (status) {
            case "RUNNING" -> NamedTextColor.GREEN;
            case "STARTING", "STOPPING" -> NamedTextColor.GOLD;
            default -> NamedTextColor.RED;
        };
    }

    private Component header(String text) {
        return Component.text(text, NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    private Component line(String label, String value, NamedTextColor valueColor) {
        return Component.text()
                .append(Component.text(label + ": ", NamedTextColor.GRAY))
                .append(Component.text(value, valueColor))
                .build();
    }

    private Component info(String text) {
        return Component.text(text, NamedTextColor.GOLD);
    }

    private Component success(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    private Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }

    private Component muted(String text) {
        return Component.text(text, NamedTextColor.DARK_GRAY);
    }

    private Component secondary(String text) {
        return Component.text(text, NamedTextColor.GRAY);
    }

    private Component missingPermission(String permission) {
        return error(t("command.missing_permission", Map.of("permission", permission)));
    }

    private TranslationService translations() {
        return plugin.requireRuntime().translations();
    }

    private String t(String key) {
        return translations().text(key);
    }

    private String t(String key, Map<String, ?> arguments) {
        return translations().text(key, arguments);
    }
}
