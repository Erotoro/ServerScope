package com.serverscope.storage.sqlite.repository;

import com.serverscope.api.storage.MetricSample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqliteMetricSampleRepository {
    public void insertBatch(Connection connection, List<MetricSample> samples) throws SQLException {
        if (samples.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO metric_samples(
                    sample_time,
                    tps,
                    mspt,
                    heap_used_bytes,
                    online_players,
                    world_count,
                    loaded_chunks,
                    total_entities
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (MetricSample sample : samples) {
                statement.setLong(1, sample.sampleTime().toEpochMilli());
                statement.setDouble(2, sample.tps());
                statement.setDouble(3, sample.mspt());
                statement.setLong(4, sample.heapUsedBytes());
                statement.setInt(5, sample.onlinePlayers());
                statement.setInt(6, sample.worldCount());
                statement.setLong(7, sample.loadedChunks());
                statement.setLong(8, sample.totalEntities());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<MetricSample> findLatest(Connection connection, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sample_time, tps, mspt, heap_used_bytes, online_players, world_count, loaded_chunks, total_entities
                FROM metric_samples
                ORDER BY sample_time DESC
                LIMIT ?
                """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MetricSample> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new MetricSample(
                            Instant.ofEpochMilli(resultSet.getLong("sample_time")),
                            resultSet.getDouble("tps"),
                            resultSet.getDouble("mspt"),
                            resultSet.getLong("heap_used_bytes"),
                            resultSet.getInt("online_players"),
                            resultSet.getInt("world_count"),
                            resultSet.getLong("loaded_chunks"),
                            resultSet.getLong("total_entities")
                    ));
                }
                return result;
            }
        }
    }

    public List<MetricSample> findSince(Connection connection, Instant since, int limit) throws SQLException {
        // Pull the most recent `limit` rows within the window (uses idx_metric_samples_time),
        // then re-order ascending so callers get a ready-to-plot time series.
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sample_time, tps, mspt, heap_used_bytes, online_players, world_count, loaded_chunks, total_entities
                FROM (
                    SELECT sample_time, tps, mspt, heap_used_bytes, online_players, world_count, loaded_chunks, total_entities
                    FROM metric_samples
                    WHERE sample_time >= ?
                    ORDER BY sample_time DESC
                    LIMIT ?
                )
                ORDER BY sample_time ASC
                """)) {
            statement.setLong(1, since.toEpochMilli());
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MetricSample> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new MetricSample(
                            Instant.ofEpochMilli(resultSet.getLong("sample_time")),
                            resultSet.getDouble("tps"),
                            resultSet.getDouble("mspt"),
                            resultSet.getLong("heap_used_bytes"),
                            resultSet.getInt("online_players"),
                            resultSet.getInt("world_count"),
                            resultSet.getLong("loaded_chunks"),
                            resultSet.getLong("total_entities")
                    ));
                }
                return result;
            }
        }
    }

    public int deleteOlderThan(Connection connection, Instant cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM metric_samples WHERE sample_time < ?"
        )) {
            statement.setLong(1, cutoff.toEpochMilli());
            return statement.executeUpdate();
        }
    }
}
