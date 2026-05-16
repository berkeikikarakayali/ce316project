package com.ce316.iae.service;

import com.ce316.iae.dao.EvaluationResultDAO;
import com.ce316.iae.model.AssignmentReport;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.StudentReport;
import com.ce316.iae.model.Submission;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class ReportingService {

    private final EvaluationResultDAO dao;
    private final List<StudentReport> pending = new ArrayList<>();

    public ReportingService(EvaluationResultDAO dao) {
        this.dao = dao;
    }

    public void clearPending() {
        pending.clear();
    }

    public List<StudentReport> getPendingSnapshot() {
        return List.copyOf(pending);
    }

    public void addReport(Submission submission,
                          ComparisonStatus status,
                          String actualOutput,
                          String expectedOutput,
                          List<String> diffLines,
                          String errorMessage,
                          NormalizationMode normalizationMode) {
        StudentReport r = new StudentReport();
        r.setStudentSubmissionId(submission.getId());
        r.setStudentId(submission.getStudentId());
        r.setStatus(status);
        r.setActualOutput(actualOutput);
        r.setExpectedOutput(expectedOutput);
        if (diffLines != null && !diffLines.isEmpty()) {
            r.setDiffLines(diffLines);
        }
        r.setErrorMessage(errorMessage);
        r.setNormalizationMode(normalizationMode);
        r.setTimestamp(Instant.now().toString());
        pending.add(r);
    }

    public void saveToProject() throws SQLException {
        dao.deleteAll();
        dao.insertAll(new ArrayList<>(pending));
        pending.clear();
    }

    public AssignmentReport summarize(List<StudentReport> rows) {
        int pass = 0;
        int fail = 0;
        int error = 0;
        if (rows != null) {
            for (StudentReport r : rows) {
                ComparisonStatus s = r.getStatus();
                if (s == ComparisonStatus.PASS) {
                    pass++;
                } else if (s == ComparisonStatus.FAIL) {
                    fail++;
                } else {
                    error++;
                }
            }
        }
        int total = rows == null ? 0 : rows.size();
        return new AssignmentReport(total, pass, fail, error);
    }

    public void exportCsv(Path outputPath, List<StudentReport> rows) throws IOException {
        String header = "studentId,status,normalization,timestamp,message,actualPreview,diffLineCount";
        StringBuilder sb = new StringBuilder();
        sb.append(header).append('\n');
        if (rows != null) {
            for (StudentReport r : rows) {
                sb.append(r.toCsvRow()).append('\n');
            }
        }
        Files.writeString(outputPath, sb.toString(), StandardCharsets.UTF_8);
    }
}
