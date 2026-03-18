package com.serverscope.storage.sqlite.repository;

import com.serverscope.api.storage.PluginProfileSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqlitePluginProfileSnapshotRepository {
    public void insertBatch(Connection connection, List<PluginProfileSnapshot> snapshots) throws SQLException {
        if (snapshots.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO plugin_profile_snapshots(
                    sample_time,
                    plugin_name,
                    event_name,
                    listener_class,
                    calls_count,
                    total_time_nanos,
                    max_time_nanos,
                    p95_time_nanos
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (PluginProfileSnapshot snapshot : snapshots) {
                statement.setLong(1, snapshot.sampleTime().toEpochMilli());
                statement.setString(2, snapshot.pluginName());
                statement.setString(3, snapshot.eventName());
                statement.setString(4, snapshot.listenerClass());
                statement.setLong(5, snapshot.callsCount());
                statement.setLong(6, snapshot.totalTimeNanos());
                statement.setLong(7, snapshot.maxTimeNanos());
                statement.setLong(8, snapshot.p95TimeNanos());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<PluginProfileSnapshot> findLatest(Connection connection, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sample_time, plugin_name, event_name, listener_class, calls_count, total_time_nanos, max_time_nanos, p95_time_nanos
                FROM plugin_profile_snapshots
                ORDER BY sample_time DESC
                LIMIT ?
                """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<PluginProfileSnapshot> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new PluginProfileSnapshot(
                            Instant.ofEpochMilli(resultSet.getLong("sample_time")),
                            resultSet.getString("plugin_name"),
                            resultSet.getString("event_name"),
                            resultSet.getString("listener_class"),
                            resultSet.getLong("calls_count"),
                            resultSet.getLong("total_time_nanos"),
                            resultSet.getLong("max_time_nanos"),
                            resultSet.getLong("p95_time_nanos")
                    ));
                }
                return result;
            }
        }
    }

    public int deleteOlderThan(Connection connection, Instant cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM plugin_profile_snapshots WHERE sample_time < ?"
        )) {
            statement.setLong(1, cutoff.toEpochMilli());
            return statement.executeUpdate();
        }
    }
}
