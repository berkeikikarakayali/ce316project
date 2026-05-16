package com.ce316.iae.model;

public final class ValidationResult {
    private final ConfigStatus status;
    private final String message;

    public ValidationResult(ConfigStatus status, String message) {
        this.status = status != null ? status : ConfigStatus.INVALID_ARGS;
        this.message = message != null ? message : "";
    }

    public boolean isValid() {
        return status == ConfigStatus.VALID;
    }

    public ConfigStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public static ValidationResult ok() {
        return new ValidationResult(ConfigStatus.VALID, "");
    }
}
