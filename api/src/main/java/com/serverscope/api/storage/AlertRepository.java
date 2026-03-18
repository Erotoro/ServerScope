package com.serverscope.api.storage;

import java.util.List;

public interface AlertRepository {
    List<AlertRecord> findLatestAlerts(int limit);
}
