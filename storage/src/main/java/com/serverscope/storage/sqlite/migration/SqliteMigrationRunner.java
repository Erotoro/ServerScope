package com.serverscope.storage.sqlite.migration;

import com.serverscope.storage.sqlite.SqliteConnectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class SqliteMigrationRunner {
    private static final List<String> MIGRATIONS = List.of(
            "db/migration/V1__init.sql",
            "db/migration/V2__world_and_findings.sql",
            "db/migration/V3__event_profile_snapshots.sql"
    );

    private final SqliteConnectionFactory connectionFactory;

    public SqliteMigrationRunner(SqliteConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void migrate() throws SQLException {
        try (Connection connection = connectionFactory.openConnection()) {
            createMetadataTable(connection);

            for (String migrationPath : MIGRATIONS) {
                String version = versionOf(migrationPath);
                if (isApplied(connection, version)) {
                    continue;
                }

                String sql = loadMigration(migrationPath);
                connection.setAutoCommit(false);
                try (Statement statement = connection.createStatement()) {
                    for (String sqlStatement : splitStatements(sql)) {
                        if (!sqlStatement.isBlank()) {
                            statement.execute(sqlStatement);
                        }
                    }
                    recordMigration(connection, version, migrationPath);
                    connection.commit();
                } catch (SQLException exception) {
                    connection.rollback();
                    throw exception;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
    }

    private void createMetadataTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version TEXT PRIMARY KEY,
                        description TEXT NOT NULL,
                        applied_at INTEGER NOT NULL
                    )
                    """);
        }
    }

    private boolean isApplied(Connection connection, String version) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM schema_migrations WHERE version = ?"
        )) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void recordMigration(Connection connection, String version, String description) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO schema_migrations(version, description, applied_at) VALUES (?, ?, ?)"
        )) {
            statement.setString(1, version);
            statement.setString(2, description);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private String loadMigration(String path) throws SQLException {
        try (InputStream inputStream = SqliteMigrationRunner.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new SQLException("Migration not found: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new SQLException("Failed to load migration " + path, exception);
        }
    }

    private String versionOf(String migrationPath) {
        String fileName = migrationPath.substring(migrationPath.lastIndexOf('/') + 1);
        int separatorIndex = fileName.indexOf("__");
        return separatorIndex > 0 ? fileName.substring(0, separatorIndex) : fileName;
    }

    private List<String> splitStatements(String sql) {
        return List.of(sql.split(";"));
    }
}
