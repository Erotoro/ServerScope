package com.serverscope.storage.sqlite;

import com.serverscope.api.config.StorageConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SqliteConnectionFactory {
    private final StorageConfig config;

    public SqliteConnectionFactory(StorageConfig config) {
        this.config = config;
    }

    public Connection openConnection() throws SQLException {
        ensureParentDirectory();

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + config.sqliteFile().toAbsolutePath());
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    private void ensureParentDirectory() throws SQLException {
        Path parent = config.sqliteFile().toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }

        try {
            Files.createDirectories(parent);
        } catch (Exception exception) {
            throw new SQLException("Failed to create SQLite directory " + parent, exception);
        }
    }
}
