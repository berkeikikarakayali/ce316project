package com.ce316.iae.service;

import com.ce316.iae.dao.ConfigurationDAO;
import com.ce316.iae.db.DatabaseService;
import com.ce316.iae.model.LanguageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationServiceTest {

    @TempDir
    Path tmp;

    @Test
    void addUpdateRemoveAndActiveLookup() throws Exception {
        ConfigurationService service = new ConfigurationService();
        LanguageConfig java = config("Java", "java");
        LanguageConfig python = interpretedConfig("Python", "py");

        service.addConfig(java);
        assertEquals("Java", service.getConfig().getName());

        service.addConfig(python);
        service.setActiveLanguage("Python");
        assertEquals("Python", service.getConfig().getName());

        LanguageConfig updated = interpretedConfig("Python", "python");
        service.updateConfig(updated);
        assertEquals("python", service.getConfigForLanguage("Python").getFileExtension());

        service.removeConfig("Java");
        List<LanguageConfig> all = service.listAll();
        assertEquals(1, all.size());
        assertEquals("Python", all.get(0).getName());
    }

    @Test
    void addRejectsInvalidConfig() {
        ConfigurationService service = new ConfigurationService();
        LanguageConfig invalid = new LanguageConfig(
                "C", "c", tmp.resolve("missing").toString(),
                Collections.singletonList("{source}"),
                Collections.singletonList("{executable}"));

        assertThrows(IllegalArgumentException.class, () -> service.addConfig(invalid));
        assertTrue(service.listAll().isEmpty());
    }

    @Test
    void loadAndSaveRoundTripThroughConfigurationDao() throws Exception {
        Path iae = tmp.resolve("configs.iae");
        try (DatabaseService db = new DatabaseService()) {
            db.createNewProject(iae);
            ConfigurationDAO dao = new ConfigurationDAO(db.connection());
            ConfigurationService writer = new ConfigurationService(dao);
            writer.addConfig(config("Java", "java"));
            writer.addConfig(interpretedConfig("Python", "py"));
            writer.saveToProject();

            ConfigurationService reader = new ConfigurationService(dao);
            reader.loadFromProject();

            assertEquals(2, reader.listAll().size());
            assertEquals("java", reader.getConfigForLanguage("Java").getFileExtension());
            assertEquals("py", reader.getConfigForLanguage("Python").getFileExtension());
        }
    }

    private LanguageConfig config(String name, String extension) throws Exception {
        return new LanguageConfig(
                name, extension, tool(name + "-compiler").toString(),
                Arrays.asList("-d", "{workDir}", "{source}"),
                Collections.singletonList("{executable}"));
    }

    private LanguageConfig interpretedConfig(String name, String extension) throws Exception {
        return new LanguageConfig(
                name, extension, null,
                Collections.emptyList(),
                Arrays.asList(tool(name + "-runtime").toString(), "{source}"));
    }

    private Path tool(String name) throws Exception {
        Path path = tmp.resolve(name);
        Files.writeString(path, "");
        return path;
    }
}
