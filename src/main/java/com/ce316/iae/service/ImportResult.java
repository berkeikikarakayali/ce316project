package com.ce316.iae.service;

import com.ce316.iae.model.LanguageConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImportResult {
    private final boolean success;
    private final List<LanguageConfig> importedConfigs;
    private final List<SkippedEntry> skippedEntries;
    private final String errorMessage;

    public ImportResult(boolean success, List<LanguageConfig> importedConfigs,
                        List<SkippedEntry> skippedEntries, String errorMessage) {
        this.success = success;
        this.importedConfigs = importedConfigs != null
                ? new ArrayList<>(importedConfigs) : new ArrayList<>();
        this.skippedEntries = skippedEntries != null
                ? new ArrayList<>(skippedEntries) : new ArrayList<>();
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }

    public static ImportResult failure(String errorMessage) {
        return new ImportResult(false, Collections.emptyList(), Collections.emptyList(), errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public List<LanguageConfig> getImportedConfigs() {
        return new ArrayList<>(importedConfigs);
    }

    public List<SkippedEntry> getSkippedEntries() {
        return new ArrayList<>(skippedEntries);
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
