package com.serverscope.api.diagnostic;

public record DiagnosticReference(
        String worldName,
        Integer chunkX,
        Integer chunkZ,
        String pluginName
) {
}
