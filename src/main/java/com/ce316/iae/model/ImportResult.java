package com.ce316.iae.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ImportResult {
    private final boolean success;
    private final String errorMessage;
    private final List<LanguageConfig> imported;
    private final List<SkippedEntry> skipped;

    public ImportResult(boolean success,
                        String errorMessage,
                        List<LanguageConfig> imported,
                        List<SkippedEntry> skipped) {
        this.success = success;
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.imported = imported != null ? new ArrayList<>(imported) : new ArrayList<>();
        this.skipped = skipped != null ? new ArrayList<>(skipped) : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<LanguageConfig> getImported() {
        return Collections.unmodifiableList(imported);
    }

    public List<SkippedEntry> getSkipped() {
        return Collections.unmodifiableList(skipped);
    }

    public static ImportResult failure(String message) {
        return new ImportResult(false, message, List.of(), List.of());
    }
}
