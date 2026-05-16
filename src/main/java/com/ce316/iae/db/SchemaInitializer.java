package com.ce316.iae.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class SchemaInitializer {

    public static final int SCHEMA_VERSION = 2;
    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private SchemaInitializer() {}

    public static void initializeNew(Connection conn) throws SQLException {
        executeSchemaScript(conn);
        writeSchemaVersion(conn, SCHEMA_VERSION);
        ensureSingleProjectRow(conn);
    }

    public static void verifyExisting(Connection conn) throws SQLException {
        int found = readSchemaVersion(conn);
        if (found != SCHEMA_VERSION) {
            throw new SQLException(
                "Schema version mismatch: file has " + found +
                ", application expects " + SCHEMA_VERSION);
        }
    }

    private static void executeSchemaScript(Connection conn) throws SQLException {
        String script = loadResource();
        try (Statement stmt = conn.createStatement()) {
            for (String sql : script.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static String loadResource() throws SQLException {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new SQLException("Schema resource not found: " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new SQLException("Failed to read schema resource", e);
        }
    }

    private static void writeSchemaVersion(Connection conn, int version) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO meta(key, value) VALUES('schema_version', ?)")) {
            ps.setString(1, Integer.toString(version));
            ps.executeUpdate();
        }
    }

    private static int readSchemaVersion(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM meta WHERE key = 'schema_version'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new SQLException("schema_version row missing — file is not a valid .iae project");
            }
            try {
                return Integer.parseInt(rs.getString(1));
            } catch (NumberFormatException e) {
                throw new SQLException("schema_version value is not an integer", e);
            }
        }
    }

    private static void ensureSingleProjectRow(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT OR IGNORE INTO PROJECT(id) VALUES(1)");
        }
    }
}
