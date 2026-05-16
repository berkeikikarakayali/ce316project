package com.ce316.iae.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ComparisonResult {
    private final ComparisonStatus status;
    private final String expectedSnapshot;
    private final List<String> diffLines;

    public ComparisonResult(ComparisonStatus status, String expectedSnapshot, List<String> diffLines) {
        this.status = status != null ? status : ComparisonStatus.ERROR;
        this.expectedSnapshot = expectedSnapshot != null ? expectedSnapshot : "";
        this.diffLines = diffLines != null ? new ArrayList<>(diffLines) : new ArrayList<>();
    }

    public ComparisonStatus getStatus() {
        return status;
    }

    public String getExpectedSnapshot() {
        return expectedSnapshot;
    }

    public List<String> getDiffLines() {
        return Collections.unmodifiableList(diffLines);
    }

    public boolean isPassed() {
        return status == ComparisonStatus.PASS;
    }
}
