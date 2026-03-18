package com.serverscope.api.diagnostic;

import java.util.List;

public interface DiagnosticService {
    List<DiagnosticFinding> activeFindings();
}
