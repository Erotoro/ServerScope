package com.serverscope.api.storage;

import java.util.List;

public interface AnalyzerFindingRepository {
    List<AnalyzerFindingRecord> findLatestAnalyzerFindings(int limit);
}
