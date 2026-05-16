package com.ce316.iae.model;

public class Project {
    private String name;
    private Integer configurationId;
    private String expectedOutputPath;
    /** Folder containing student ZIP archives (batch extraction runs here). */
    private String zipFolderPath;
    /** Preferred source file name inside each extraction (e.g. main.c); optional — inferred from extension when blank. */
    private String mainSourceFilename;
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

    public String getZipFolderPath() { return zipFolderPath; }
    public void setZipFolderPath(String zipFolderPath) { this.zipFolderPath = zipFolderPath; }

    public String getMainSourceFilename() { return mainSourceFilename; }
    public void setMainSourceFilename(String mainSourceFilename) { this.mainSourceFilename = mainSourceFilename; }

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
