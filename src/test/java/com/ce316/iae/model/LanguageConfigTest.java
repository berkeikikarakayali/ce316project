package com.ce316.iae.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LanguageConfigTest {

    @TempDir
    Path tmp;

    @Test
    void validate_acceptsExistingExplicitToolPaths() throws Exception {
        Path compiler = tool("javac");
        Path runtime = tool("java");
        LanguageConfig config = new LanguageConfig(
                "Java", "java", compiler.toString(),
                Arrays.asList("-d", "{workDir}", "{source}"),
                Arrays.asList(runtime.toString(), "-cp", "{workDir}", "{className}"));

        ValidationResult result = config.validate();

        assertTrue(result.isValid());
        assertEquals(ConfigStatus.VALID, config.getStatus());
    }

    @Test
    void validate_rejectsMissingCompilerPath() {
        LanguageConfig config = new LanguageConfig(
                "C", "c", tmp.resolve("missing-gcc").toString(),
                Arrays.asList("{source}", "-o", "{executable}"),
                Collections.singletonList("{executable}"));

        ValidationResult result = config.validate();

        assertFalse(result.isValid());
        assertEquals(ConfigStatus.INVALID_PATH, result.getStatus());
        assertTrue(result.getMessage().contains("not found"));
    }

    @Test
    void validate_rejectsUnknownPlaceholder() throws Exception {
        LanguageConfig config = new LanguageConfig(
                "C", "c", tool("gcc").toString(),
                Collections.singletonList("{bad}"),
                Collections.singletonList("{executable}"));

        ValidationResult result = config.validate();

        assertFalse(result.isValid());
        assertEquals(ConfigStatus.INVALID_ARGS, result.getStatus());
        assertTrue(result.getMessage().contains("Unknown placeholder"));
    }

    @Test
    void buildCommands_supportJavaStylePlaceholdersAndQuotedProjectArgs() {
        Path workDir = tmp.resolve("work");
        Path source = workDir.resolve("Main.java");
        LanguageConfig config = new LanguageConfig(
                "Java", "java", "javac",
                Arrays.asList("-d", "{workDir}", "{source}"),
                Arrays.asList("java", "-cp", "{workDir}", "{className}"));

        List<String> compile = config.buildCompileCommand(source.toString(), workDir.toString());
        List<String> run = config.buildRunCommand(source.toString(), workDir.toString(), "\"alpha beta\" 42");

        assertEquals(Arrays.asList("javac", "-d", workDir.toString(), source.toString()), compile);
        assertEquals(Arrays.asList("java", "-cp", workDir.toString(), "Main", "alpha beta", "42"), run);
    }

    @Test
    void buildCommands_supportCExecutablePlaceholder() {
        Path workDir = tmp.resolve("work");
        Path source = workDir.resolve("main.c");
        LanguageConfig config = new LanguageConfig(
                "C", "c", "gcc",
                Arrays.asList("{source}", "-o", "{executable}"),
                Collections.singletonList("{executable}"));

        assertEquals(
                Arrays.asList("gcc", source.toString(), "-o", workDir.resolve("main").toString()),
                config.buildCompileCommand(source.toString(), workDir.toString()));
        assertEquals(
                Collections.singletonList(workDir.resolve("main").toString()),
                config.buildRunCommand(source.toString(), workDir.toString(), ""));
    }

    @Test
    void buildCommands_supportPythonSourceWithoutCompileStep() {
        Path workDir = tmp.resolve("work");
        Path source = workDir.resolve("solution.py");
        LanguageConfig config = new LanguageConfig(
                "Python", "py", null,
                Collections.emptyList(),
                Arrays.asList("python3", "{source}"));

        assertFalse(config.hasCompileStep());
        assertTrue(config.buildCompileCommand(source.toString(), workDir.toString()).isEmpty());
        assertEquals(
                Arrays.asList("python3", source.toString(), "--case", "one two"),
                config.buildRunCommand(source.toString(), workDir.toString(), "--case 'one two'"));
    }

    @Test
    void buildRunCommand_compatibilityOverloadWorksWhenNoSourcePlaceholderIsNeeded() {
        LanguageConfig config = new LanguageConfig(
                "Java", "java", "javac",
                Arrays.asList("-d", "{workDir}"),
                Arrays.asList("java", "-cp", "{workDir}", "Main"));

        assertEquals(
                Arrays.asList("java", "-cp", tmp.toString(), "Main", "arg"),
                config.buildRunCommand(tmp.toString(), "arg"));
    }

    @Test
    void buildRunCommand_rejectsUnbalancedProjectRunArgQuotes() {
        LanguageConfig config = new LanguageConfig(
                "Python", "py", null,
                Collections.emptyList(),
                Arrays.asList("python3", "{source}"));

        assertThrows(IllegalArgumentException.class,
                () -> config.buildRunCommand(tmp.resolve("main.py").toString(), tmp.toString(), "\"broken"));
    }

    private Path tool(String name) throws Exception {
        Path path = tmp.resolve(name);
        Files.writeString(path, "");
        return path;
    }
}
