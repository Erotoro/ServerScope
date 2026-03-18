package com.serverscope.storage.sqlite.repository;

import com.serverscope.api.storage.AnalyzerFindingRecord;
import com.serverscope.api.storage.AnalyzerFindingSeverity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqliteAnalyzerFindingRepository {
    public void insertBatch(Connection connection, List<AnalyzerFindingRecord> findings) throws SQLException {
        if (findings.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO analyzer_findings(
                    event_time,
                    finding_code,
                    severity,
                    subject,
                    message,
                    details_json
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            for (AnalyzerFindingRecord finding : findings) {
                statement.setLong(1, finding.eventTime().toEpochMilli());
                statement.setString(2, finding.findingCode());
                statement.setString(3, finding.severity().name());
                statement.setString(4, finding.subject());
                statement.setString(5, finding.message());
                statement.setString(6, finding.detailsJson());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<AnalyzerFindingRecord> findLatest(Connection connection, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT event_time, finding_code, severity, subject, message, details_json
                FROM analyzer_findings
                ORDER BY event_time DESC, finding_code ASC
                LIMIT ?
                """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AnalyzerFindingRecord> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new AnalyzerFindingRecord(
                            Instant.ofEpochMilli(resultSet.getLong("event_time")),
                            resultSet.getString("finding_code"),
                            AnalyzerFindingSeverity.valueOf(resultSet.getString("severity")),
                            resultSet.getString("subject"),
                            resultSet.getString("message"),
                            resultSet.getString("details_json")
                    ));
                }
                return result;
            }
        }
    }

    public int deleteOlderThan(Connection connection, Instant cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM analyzer_findings WHERE event_time < ?"
        )) {
            statement.setLong(1, cutoff.toEpochMilli());
            return statement.executeUpdate();
        }
    }
}
