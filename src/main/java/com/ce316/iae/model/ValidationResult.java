package com.ce316.iae.model;

public class ValidationResult {
    private final ConfigStatus status;
    private final String message;

    public ValidationResult(ConfigStatus status, String message) {
        this.status = status;
        this.message = message != null ? message : "";
    }

    public ConfigStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isValid() {
        return status == ConfigStatus.VALID;
    }
}
