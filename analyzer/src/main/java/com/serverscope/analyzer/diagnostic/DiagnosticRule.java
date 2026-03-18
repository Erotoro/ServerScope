package com.serverscope.analyzer.diagnostic;

import com.serverscope.api.diagnostic.DiagnosticFinding;

import java.util.Optional;

public interface DiagnosticRule {
    Optional<DiagnosticFinding> evaluate(RuleEvaluationContext context);
}
