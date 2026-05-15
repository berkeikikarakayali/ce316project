package com.ce316.iae.service;

import com.ce316.iae.model.LanguageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class ImportExportServiceTest {

    @TempDir
    Path tmp;

    @Test
    void exportWritesExpectedJsonShape() throws Exception {
        ConfigurationService service = new ConfigurationService();
        service.addConfig(config("Java", "java"));
        Path export = tmp.resolve("configs.json");

        new ImportExportService(service).exportToFile(export);

        String json = Files.readString(export);
        assertTrue(json.contains("\"name\": \"Java\""));
        assertTrue(json.contains("\"fileExtension\": \"java\""));
        assertTrue(json.contains("\"compileArgs\""));
        assertTrue(json.contains("\"runArgs\""));
    }

    @Test
    void importMergeReplacesSameLanguageAndKeepsExisting() throws Exception {
        ConfigurationService service = new ConfigurationService();
        service.addConfig(config("Java", "java"));
        service.addConfig(interpretedConfig("Python", "py"));
        Path importFile = tmp.resolve("merge.json");
        Files.writeString(importFile, "[" + configJson("Java", "jav", tool("new-javac"), tool("new-java")) + "]");

        ImportResult result = new ImportExportService(service).importFromFile(importFile, ImportMode.MERGE);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getImportedConfigs().size());
        assertEquals(2, service.listAll().size());
        assertEquals("jav", service.getConfigForLanguage("Java").getFileExtension());
        assertNotNull(service.getConfigForLanguage("Python"));
    }

    @Test
    void importReplaceDropsExistingAndReportsSkippedInvalidConfigs() throws Exception {
        ConfigurationService service = new ConfigurationService();
        service.addConfig(config("Java", "java"));
        Path validRuntime = tool("python");
        Path importFile = tmp.resolve("replace.json");
        Files.writeString(importFile, "["
                + interpretedConfigJson("Python", "py", validRuntime)
                + ","
                + configJson("Broken", "c", tmp.resolve("missing-gcc"), tool("unused-runtime"))
                + "]");

        ImportResult result = new ImportExportService(service).importFromFile(importFile, ImportMode.REPLACE);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getImportedConfigs().size());
        assertEquals(1, result.getSkippedEntries().size());
        assertNull(service.getConfigForLanguage("Java"));
        assertNotNull(service.getConfigForLanguage("Python"));
    }

    @Test
    void importMalformedJsonDoesNotMutateExistingConfigs() throws Exception {
        ConfigurationService service = new ConfigurationService();
        service.addConfig(config("Java", "java"));
        Path importFile = tmp.resolve("bad.json");
        Files.writeString(importFile, "{ bad json");

        ImportResult result = new ImportExportService(service).importFromFile(importFile, ImportMode.REPLACE);

        assertFalse(result.isSuccess());
        assertEquals(1, service.listAll().size());
        assertNotNull(service.getConfigForLanguage("Java"));
    }

    private LanguageConfig config(String name, String extension) throws Exception {
        return new LanguageConfig(
                name, extension, tool(name + "-compiler").toString(),
                Arrays.asList("-d", "{workDir}", "{source}"),
                Arrays.asList(tool(name + "-runtime").toString(), "-cp", "{workDir}", "{className}"));
    }

    private LanguageConfig interpretedConfig(String name, String extension) throws Exception {
        return new LanguageConfig(
                name, extension, null,
                Collections.emptyList(),
                Arrays.asList(tool(name + "-runtime").toString(), "{source}"));
    }

    private String configJson(String name, String extension, Path compiler, Path runtime) {
        return "{"
                + "\"name\":" + q(name) + ","
                + "\"fileExtension\":" + q(extension) + ","
                + "\"compilerPath\":" + q(compiler.toString()) + ","
                + "\"compileArgs\":[\"-d\",\"{workDir}\",\"{source}\"],"
                + "\"runArgs\":[" + q(runtime.toString()) + ",\"-cp\",\"{workDir}\",\"{className}\"]"
                + "}";
    }

    private String interpretedConfigJson(String name, String extension, Path runtime) {
        return "{"
                + "\"name\":" + q(name) + ","
                + "\"fileExtension\":" + q(extension) + ","
                + "\"compilerPath\":null,"
                + "\"compileArgs\":[],"
                + "\"runArgs\":[" + q(runtime.toString()) + ",\"{source}\"]"
                + "}";
    }

    private String q(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private Path tool(String name) throws Exception {
        Path path = tmp.resolve(name);
        Files.writeString(path, "");
        return path;
    }
}
