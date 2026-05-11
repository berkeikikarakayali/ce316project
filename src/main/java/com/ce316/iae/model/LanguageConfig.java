package com.ce316.iae.model;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LanguageConfig)) return false;
        LanguageConfig that = (LanguageConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
