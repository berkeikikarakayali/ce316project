package com.ce316.iae.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection to a single .iae project file.
 * Each lecturer-facing project lives in one SQLite database; only one is
 * open at a time. Backs Controller.onNewProject / onOpenProject / onSaveProject.
 */
public class DatabaseService implements AutoCloseable {

    private Connection connection;
    private Path currentProjectPath;

    public synchronized void createNewProject(Path iaeFile) throws SQLException {
        if (iaeFile == null) {
            throw new IllegalArgumentException("iaeFile must not be null");
        }
        closeProject();
        try {
            Files.deleteIfExists(iaeFile);
            Path parent = iaeFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            throw new SQLException("Failed to prepare .iae file path: " + iaeFile, e);
        }
        connection = openConnection(iaeFile);
        SchemaInitializer.initializeNew(connection);
        currentProjectPath = iaeFile;
    }

    public synchronized void openProject(Path iaeFile) throws SQLException {
        if (iaeFile == null) {
            throw new IllegalArgumentException("iaeFile must not be null");
        }
        if (!Files.isRegularFile(iaeFile)) {
            throw new SQLException("Project file does not exist: " + iaeFile);
        }
        closeProject();
        connection = openConnection(iaeFile);
        SchemaInitializer.verifyExisting(connection);
        currentProjectPath = iaeFile;
    }

    public synchronized void saveProject() throws SQLException {
        requireOpen();
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    public synchronized void closeProject() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // best-effort close
            }
            connection = null;
        }
        currentProjectPath = null;
    }

    public synchronized boolean isOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized Path currentProjectPath() {
        return currentProjectPath;
    }

    public synchronized Connection connection() {
        requireOpen();
        return connection;
    }

    @Override
    public void close() {
        closeProject();
    }

    private void requireOpen() {
        if (connection == null) {
            throw new IllegalStateException("No project is currently open");
        }
    }

    private static Connection openConnection(Path iaeFile) throws SQLException {
        String url = "jdbc:sqlite:" + iaeFile.toAbsolutePath();
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }
}
