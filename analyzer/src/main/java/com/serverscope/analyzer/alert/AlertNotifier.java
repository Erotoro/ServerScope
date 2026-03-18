package com.serverscope.analyzer.alert;

import com.serverscope.api.alert.AlertRecord;

public interface AlertNotifier {
    void notify(AlertRecord alertRecord);
}
