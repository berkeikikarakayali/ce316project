package com.ce316.iae.model;

public class Project {
    private String name;
    private Integer configurationId;
    private String expectedOutputPath;
    private String runArgs;
    private int compileTimeoutSec = 60;
    private int runTimeoutSec = 30;
    private NormalizationMode normalizationMode = NormalizationMode.STRICT;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getConfigurationId() { return configurationId; }
    public void setConfigurationId(Integer configurationId) { this.configurationId = configurationId; }

    public String getExpectedOutputPath() { return expectedOutputPath; }
    public void setExpectedOutputPath(String expectedOutputPath) { this.expectedOutputPath = expectedOutputPath; }

    public String getRunArgs() { return runArgs; }
    public void setRunArgs(String runArgs) { this.runArgs = runArgs; }

    public int getCompileTimeoutSec() { return compileTimeoutSec; }
    public void setCompileTimeoutSec(int compileTimeoutSec) { this.compileTimeoutSec = compileTimeoutSec; }

    public int getRunTimeoutSec() { return runTimeoutSec; }
    public void setRunTimeoutSec(int runTimeoutSec) { this.runTimeoutSec = runTimeoutSec; }

    public NormalizationMode getNormalizationMode() { return normalizationMode; }
    public void setNormalizationMode(NormalizationMode normalizationMode) {
        this.normalizationMode = normalizationMode != null ? normalizationMode : NormalizationMode.STRICT;
    }
}
