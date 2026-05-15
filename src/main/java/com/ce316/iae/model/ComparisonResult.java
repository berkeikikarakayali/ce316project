package com.ce316.iae.model;

import java.util.ArrayList;
import java.util.List;

public class ComparisonResult {

    private final ComparisonStatus status;
    private final String actualOutput;
    private final String expectedOutput;
    private final List<String> diffLines;

    public ComparisonResult(ComparisonStatus status,
                            String actualOutput,
                            String expectedOutput,
                            List<String> diffLines) {
        this.status         = status;
        this.actualOutput   = actualOutput;
        this.expectedOutput = expectedOutput;
        this.diffLines      = diffLines != null ? new ArrayList<>(diffLines) : new ArrayList<>();
    }

    public ComparisonStatus getStatus()       { return status; }
    public String           getActualOutput() { return actualOutput; }
    public String           getExpectedOutput(){ return expectedOutput; }
    public List<String>     getDiffLines()    { return diffLines; }

    public boolean isPassed() {
        return status == ComparisonStatus.PASS;
    }

    public String getSummary() {
        switch (status) {
            case PASS:          return "Output matches expected.";
            case FAIL:          return "Output differs on " + diffLines.size() + " line(s).";
            case ERROR:         return "Comparison error.";
            case COMPILE_ERROR: return "Compilation failed.";
            case TIMEOUT:       return "Process timed out.";
            default:            return status.name();
        }
    }
}
