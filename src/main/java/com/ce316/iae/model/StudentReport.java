package com.ce316.iae.model;

import java.util.ArrayList;
import java.util.List;

public class StudentReport {
    private Integer id;
    private Integer studentSubmissionId;
    /** Populated when loading joined queries for UI display (not persisted). */
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

    /**
     * CSV row matching design §6: studentId, status, normalizationMode, timestamp,
     * message preview, actual output preview, diff line count.
     */
    public String toCsvRow() {
        String previewActual = actualOutput == null ? ""
                : actualOutput.replace('\r', ' ').replace('\n', ' ');
        if (previewActual.length() > 120) {
            previewActual = previewActual.substring(0, 117) + "...";
        }
        String msg = errorMessage == null ? "" : errorMessage.replace('\r', ' ').replace('\n', ' ');
        if (msg.length() > 120) {
            msg = msg.substring(0, 117) + "...";
        }
        String sid = studentId != null ? studentId : "";
        String norm = normalizationMode != null ? normalizationMode.name() : "";
        int diffCount = diffLines != null ? diffLines.size() : 0;
        return String.join(",",
                csvEscape(sid),
                csvEscape(status != null ? status.name() : ""),
                csvEscape(norm),
                csvEscape(timestamp != null ? timestamp : ""),
                csvEscape(msg),
                csvEscape(previewActual),
                Integer.toString(diffCount));
    }

    private static String csvEscape(String s) {
        if (s == null) return "\"\"";
        String escaped = s.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
