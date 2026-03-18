package com.serverscope.storage.sqlite.repository;

import com.serverscope.api.storage.EventProfileSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqliteEventProfileSnapshotRepository {
    public void insertBatch(Connection connection, List<EventProfileSnapshot> snapshots) throws SQLException {
        if (snapshots.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO event_profile_snapshots(
                    sample_time,
                    snapshot_kind,
                    event_id,
                    event_class_name,
                    count,
                    total_time_nanos,
                    max_time_nanos,
                    average_time_nanos,
                    max_window_count,
                    burst_score
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (EventProfileSnapshot snapshot : snapshots) {
                statement.setLong(1, snapshot.sampleTime().toEpochMilli());
                statement.setString(2, snapshot.snapshotKind());
                statement.setString(3, snapshot.eventId());
                statement.setString(4, snapshot.eventClassName());
                statement.setLong(5, snapshot.count());
                statement.setLong(6, snapshot.totalTimeNanos());
                statement.setLong(7, snapshot.maxTimeNanos());
                statement.setLong(8, snapshot.averageTimeNanos());
                statement.setLong(9, snapshot.maxWindowCount());
                statement.setDouble(10, snapshot.burstScore());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<EventProfileSnapshot> findLatest(Connection connection, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sample_time, snapshot_kind, event_id, event_class_name, count, total_time_nanos, max_time_nanos,
                       average_time_nanos, max_window_count, burst_score
                FROM event_profile_snapshots
                ORDER BY sample_time DESC, snapshot_kind ASC, event_id ASC
                LIMIT ?
                """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<EventProfileSnapshot> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new EventProfileSnapshot(
                            Instant.ofEpochMilli(resultSet.getLong("sample_time")),
                            resultSet.getString("snapshot_kind"),
                            resultSet.getString("event_id"),
                            resultSet.getString("event_class_name"),
                            resultSet.getLong("count"),
                            resultSet.getLong("total_time_nanos"),
                            resultSet.getLong("max_time_nanos"),
                            resultSet.getLong("average_time_nanos"),
                            resultSet.getLong("max_window_count"),
                            resultSet.getDouble("burst_score")
                    ));
                }
                return result;
            }
        }
    }

    public int deleteOlderThan(Connection connection, Instant cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM event_profile_snapshots WHERE sample_time < ?"
        )) {
            statement.setLong(1, cutoff.toEpochMilli());
            return statement.executeUpdate();
        }
    }
}
