package com.ce316.iae.model;

public final class SkippedEntry {
    private final LanguageConfig config;
    private final String reason;

    public SkippedEntry(LanguageConfig config, String reason) {
        this.config = config;
        this.reason = reason != null ? reason : "";
    }

    public LanguageConfig getConfig() {
        return config;
    }

    public String getReason() {
        return reason;
    }
}
