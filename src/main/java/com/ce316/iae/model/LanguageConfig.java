package com.ce316.iae.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LanguageConfig {
    private Integer id;
    private String name;
    private String fileExtension;
    private String compilerPath;
    private List<String> compileArgs;
    private List<String> runArgs;

    public LanguageConfig() {
        this.compileArgs = new ArrayList<>();
        this.runArgs = new ArrayList<>();
    }

    public LanguageConfig(String name, String fileExtension, String compilerPath,
                          List<String> compileArgs, List<String> runArgs) {
        this.name = name;
        this.fileExtension = fileExtension;
        this.compilerPath = compilerPath;
        this.compileArgs = compileArgs != null ? new ArrayList<>(compileArgs) : new ArrayList<>();
        this.runArgs = runArgs != null ? new ArrayList<>(runArgs) : new ArrayList<>();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public String getCompilerPath() { return compilerPath; }
    public void setCompilerPath(String compilerPath) { this.compilerPath = compilerPath; }

    public List<String> getCompileArgs() { return compileArgs; }
    public void setCompileArgs(List<String> compileArgs) {
        this.compileArgs = compileArgs != null ? new ArrayList<>(compileArgs) : new ArrayList<>();
    }

    public List<String> getRunArgs() { return runArgs; }
    public void setRunArgs(List<String> runArgs) {
        this.runArgs = runArgs != null ? new ArrayList<>(runArgs) : new ArrayList<>();
    }

    /** True when this configuration performs an explicit compile step before run. */
    public boolean hasCompileStep() {
        return compileArgs != null && !compileArgs.isEmpty()
                && compilerPath != null && !compilerPath.trim().isEmpty();
    }

    /**
     * Validates configuration enough for typical lecturer workflows.
     * Interpreted languages use an empty compile-arg list (no compile step).
     */
    public ValidationResult validate() {
        if (name == null || name.isBlank()) {
            return new ValidationResult(ConfigStatus.INVALID_ARGS, "Language name must not be empty.");
        }
        if (fileExtension == null || fileExtension.isBlank()) {
            return new ValidationResult(ConfigStatus.INVALID_ARGS, "File extension must not be empty.");
        }
        if (runArgs == null || runArgs.isEmpty()) {
            return new ValidationResult(ConfigStatus.INVALID_ARGS, "Run arguments must not be empty.");
        }
        if (hasCompileStep()) {
            Path cp = Paths.get(compilerPath.trim());
            if (!Files.exists(cp)) {
                return new ValidationResult(ConfigStatus.INVALID_PATH,
                        "Compiler not found at " + compilerPath);
            }
        }
        return ValidationResult.ok();
    }

    public List<String> buildCompileCommand(String mainSourceFile, String extractedFolderPath) {
        Path src = Paths.get(mainSourceFile);
        Path dir = Paths.get(extractedFolderPath);
        List<String> cmd = new ArrayList<>();
        cmd.add(compilerPath.trim());
        for (String arg : compileArgs) {
            cmd.add(substituteTokens(arg, src, dir));
        }
        return cmd;
    }

    public List<String> buildRunCommand(String extractedFolderPath,
                                        List<String> projectRunArgs,
                                        String mainSourceFileOrNull) {
        Path dir = Paths.get(extractedFolderPath);
        Path src = mainSourceFileOrNull != null ? Paths.get(mainSourceFileOrNull) : null;
        List<String> cmd = new ArrayList<>();
        for (String arg : runArgs) {
            cmd.add(substituteTokens(arg, src, dir));
        }
        if (projectRunArgs != null && !projectRunArgs.isEmpty()) {
            cmd.addAll(projectRunArgs);
        }
        return cmd;
    }

    private static String substituteTokens(String raw, Path mainSourceFile, Path workingDir) {
        if (raw == null) {
            return "";
        }
        String dirStr = "";
        if (workingDir != null) {
            dirStr = workingDir.toAbsolutePath().normalize().toString();
        }
        String srcStr = "";
        String baseStr = "";
        if (mainSourceFile != null) {
            Path abs = mainSourceFile.toAbsolutePath().normalize();
            srcStr = abs.toString();
            Path fn = abs.getFileName();
            baseStr = fn == null ? "" : fn.toString();
        }
        return raw
                .replace("{DIR}", dirStr)
                .replace("{SRC}", srcStr)
                .replace("{BASE}", baseStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LanguageConfig)) return false;
        LanguageConfig that = (LanguageConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
