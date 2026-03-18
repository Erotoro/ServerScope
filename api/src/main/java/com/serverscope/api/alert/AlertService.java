package com.serverscope.api.alert;

import java.util.List;

public interface AlertService {
    List<AlertRecord> activeAlerts();
}
