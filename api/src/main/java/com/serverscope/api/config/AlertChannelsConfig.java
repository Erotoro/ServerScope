package com.serverscope.api.config;

public record AlertChannelsConfig(
        boolean consoleEnabled,
        boolean inGameEnabled,
        boolean webhookEnabled,
        String webhookUrl,
        String adminPermission
) {
    public AlertChannelsConfig {
        if (webhookUrl == null) {
            webhookUrl = "";
        }
        if (adminPermission == null || adminPermission.isBlank()) {
            adminPermission = "serverscope.alerts";
        }
    }
}
