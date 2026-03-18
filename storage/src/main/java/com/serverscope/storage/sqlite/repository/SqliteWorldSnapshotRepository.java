package com.serverscope.storage.sqlite.repository;

import com.serverscope.api.storage.WorldSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqliteWorldSnapshotRepository {
    public void insertBatch(Connection connection, List<WorldSnapshot> snapshots) throws SQLException {
        if (snapshots.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO world_snapshots(
                    sample_time,
                    world_name,
                    loaded_chunks
                ) VALUES (?, ?, ?)
                """)) {
            for (WorldSnapshot snapshot : snapshots) {
                statement.setLong(1, snapshot.sampleTime().toEpochMilli());
                statement.setString(2, snapshot.worldName());
                statement.setLong(3, snapshot.loadedChunks());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<WorldSnapshot> findLatest(Connection connection, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sample_time, world_name, loaded_chunks
                FROM world_snapshots
                ORDER BY sample_time DESC, world_name ASC
                LIMIT ?
                """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<WorldSnapshot> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new WorldSnapshot(
                            Instant.ofEpochMilli(resultSet.getLong("sample_time")),
                            resultSet.getString("world_name"),
                            resultSet.getLong("loaded_chunks")
                    ));
                }
                return result;
            }
        }
    }

    public int deleteOlderThan(Connection connection, Instant cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM world_snapshots WHERE sample_time < ?"
        )) {
            statement.setLong(1, cutoff.toEpochMilli());
            return statement.executeUpdate();
        }
    }
}
