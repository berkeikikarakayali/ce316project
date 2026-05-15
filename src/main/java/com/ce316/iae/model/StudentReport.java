package com.ce316.iae.model;

import java.util.ArrayList;
import java.util.List;

public class StudentReport {
    private Integer id;
    private Integer studentSubmissionId;
    private String studentId;
    private ComparisonStatus status;
    private String actualOutput;
    private String expectedOutput;
    private List<String> diffLines;
    private String errorMessage;
    private NormalizationMode normalizationMode;
    private String timestamp;

    public StudentReport() {
        this.diffLines = new ArrayList<>();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getStudentSubmissionId() { return studentSubmissionId; }
    public void setStudentSubmissionId(Integer studentSubmissionId) { this.studentSubmissionId = studentSubmissionId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public ComparisonStatus getStatus() { return status; }
    public void setStatus(ComparisonStatus status) { this.status = status; }

    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public List<String> getDiffLines() { return diffLines; }
    public void setDiffLines(List<String> diffLines) {
        this.diffLines = diffLines != null ? new ArrayList<>(diffLines) : new ArrayList<>();
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public NormalizationMode getNormalizationMode() { return normalizationMode; }
    public void setNormalizationMode(NormalizationMode normalizationMode) { this.normalizationMode = normalizationMode; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String toCSVRow() {
        String preview = actualOutput != null
                ? actualOutput.replace("\n", " ").replace("\r", "")
                : "";
        if (preview.length() > 100) preview = preview.substring(0, 100) + "...";

        return String.join(",",
                csvEscape(studentId),
                csvEscape(status != null ? status.name() : ""),
                csvEscape(normalizationMode != null ? normalizationMode.name() : ""),
                csvEscape(timestamp),
                csvEscape(errorMessage),
                csvEscape(preview),
                String.valueOf(diffLines != null ? diffLines.size() : 0)
        );
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
