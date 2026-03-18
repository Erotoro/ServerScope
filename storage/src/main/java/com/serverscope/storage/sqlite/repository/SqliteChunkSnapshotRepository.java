package com.serverscope.storage.sqlite.repository;

import com.serverscope.api.storage.ChunkSnapshot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class SqliteChunkSnapshotRepository {
    public void insertBatch(Connection connection, List<ChunkSnapshot> snapshots) throws SQLException {
        if (snapshots.isEmpty()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO chunk_snapshots(
                    sample_time,
                    world_name,
                    chunk_x,
                    chunk_z,
                    entity_count,
                    block_entity_count,
                    hotspot_score
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """)) {
            for (ChunkSnapshot snapshot : snapshots) {
                statement.setLong(1, snapshot.sampleTime().toEpochMilli());
                statement.setString(2, snapshot.worldName());
                statement.setInt(3, snapshot.chunkX());
                statement.setInt(4, snapshot.chunkZ());
                statement.setInt(5, snapshot.entityCount());
                statement.setInt(6, snapshot.blockEntityCount());
                statement.setLong(7, snapshot.hotspotScore());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    public List<ChunkSnapshot> findLatest(Connection connection, int limit) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT sample_time, world_name, chunk_x, chunk_z, entity_count, block_entity_count, hotspot_score
                FROM chunk_snapshots
                ORDER BY sample_time DESC
                LIMIT ?
                """)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ChunkSnapshot> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(new ChunkSnapshot(
                            Instant.ofEpochMilli(resultSet.getLong("sample_time")),
                            resultSet.getString("world_name"),
                            resultSet.getInt("chunk_x"),
                            resultSet.getInt("chunk_z"),
                            resultSet.getInt("entity_count"),
                            resultSet.getInt("block_entity_count"),
                            resultSet.getLong("hotspot_score")
                    ));
                }
                return result;
            }
        }
    }

    public int deleteOlderThan(Connection connection, Instant cutoff) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM chunk_snapshots WHERE sample_time < ?"
        )) {
            statement.setLong(1, cutoff.toEpochMilli());
            return statement.executeUpdate();
        }
    }
}
