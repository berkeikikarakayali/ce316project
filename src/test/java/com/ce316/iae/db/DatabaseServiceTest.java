package com.ce316.iae.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseServiceTest {

    private DatabaseService service;

    @AfterEach
    void tearDown() {
        if (service != null) service.close();
    }

    @Test
    void createNewProject_createsFileAndAllTables(@TempDir Path tmp) throws Exception {
        Path iae = tmp.resolve("test.iae");
        service = new DatabaseService();
        service.createNewProject(iae);

        assertTrue(Files.isRegularFile(iae), "iae file must exist on disk");
        assertTrue(service.isOpen());
        assertEquals(iae, service.currentProjectPath());

        assertTrue(tableExists("CONFIGURATION"));
        assertTrue(tableExists("PROJECT"));
        assertTrue(tableExists("STUDENT_SUBMISSION"));
        assertTrue(tableExists("EVALUATION_RESULT"));
        assertTrue(tableExists("meta"));

        try (PreparedStatement ps = service.connection().prepareStatement(
                "SELECT COUNT(*) FROM PROJECT WHERE id = 1");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertEquals(1, rs.getInt(1), "blank PROJECT row must be inserted");
        }
    }

    @Test
    void openProject_succeedsAfterCreateAndClose(@TempDir Path tmp) throws Exception {
        Path iae = tmp.resolve("roundtrip.iae");
        service = new DatabaseService();
        service.createNewProject(iae);
        service.closeProject();
        assertFalse(service.isOpen());

        service.openProject(iae);
        assertTrue(service.isOpen());
        assertEquals(iae, service.currentProjectPath());
    }

    @Test
    void openProject_rejectsMissingFile(@TempDir Path tmp) {
        Path missing = tmp.resolve("does-not-exist.iae");
        service = new DatabaseService();
        SQLException ex = assertThrows(SQLException.class, () -> service.openProject(missing));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void openProject_rejectsBadSchemaVersion(@TempDir Path tmp) throws Exception {
        Path iae = tmp.resolve("bad.iae");
        service = new DatabaseService();
        service.createNewProject(iae);
        try (PreparedStatement ps = service.connection().prepareStatement(
                "UPDATE meta SET value = '99' WHERE key = 'schema_version'")) {
            ps.executeUpdate();
        }
        service.closeProject();

        SQLException ex = assertThrows(SQLException.class, () -> service.openProject(iae));
        assertTrue(ex.getMessage().contains("Schema version mismatch"));
    }

    @Test
    void connection_throwsWhenNoProjectOpen() {
        service = new DatabaseService();
        assertThrows(IllegalStateException.class, service::connection);
    }

    @Test
    void foreignKeysAreEnabled(@TempDir Path tmp) throws Exception {
        Path iae = tmp.resolve("fk.iae");
        service = new DatabaseService();
        service.createNewProject(iae);
        try (PreparedStatement ps = service.connection().prepareStatement("PRAGMA foreign_keys");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    private boolean tableExists(String name) throws SQLException {
        try (PreparedStatement ps = service.connection().prepareStatement(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
