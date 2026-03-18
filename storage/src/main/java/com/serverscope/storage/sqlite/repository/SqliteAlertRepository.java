package com.serverscope.storage.sqlite.repository;

import com.serverscope.api.storage.AlertRecord;
import com.serverscope.api.storage.AlertSeverity;
import com.serverscope.api.storage.AlertStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqliteAlertRepository {
    public void insertBatch(Connection connection, List<AlertRecord> alerts) throws SQLException {
        if (alerts.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO alerts(
                    event_time,
                    alert_code,
                    severity,
                    status,
                    dedupe_key,
                    message,
                    dimensions_json
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (AlertRecord alert : alerts) {
                statement.setLong(1, alert.eventTime().toEpochMilli());
                statement.setString(2, alert.alertCode());
                statement.setString(3, alert.severity().name());
                statement.setString(4, alert.status().name());
                statement.setString(5, alert.dedupeKey());
                statement.setString(6, alert.message());
                statement.setString(7, alert.dimensionsJson());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<AlertRecord> findLatest(Connection connection, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT event_time, alert_code, severity, status, dedupe_key, message, dimensions_json
                FROM alerts
                ORDER BY event_time DESC
                LIMIT ?
                """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AlertRecord> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new AlertRecord(
                            Instant.ofEpochMilli(resultSet.getLong("event_time")),
                            resultSet.getString("alert_code"),
                            AlertSeverity.valueOf(resultSet.getString("severity")),
                            AlertStatus.valueOf(resultSet.getString("status")),
                            resultSet.getString("dedupe_key"),
                            resultSet.getString("message"),
                            resultSet.getString("dimensions_json")
                    ));
                }
                return result;
            }
        }
    }

    public int deleteOlderThan(Connection connection, Instant cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM alerts WHERE event_time < ?"
        )) {
            statement.setLong(1, cutoff.toEpochMilli());
            return statement.executeUpdate();
        }
    }
}
