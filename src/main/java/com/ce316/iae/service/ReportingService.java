package com.ce316.iae.service;

import com.ce316.iae.dao.EvaluationResultDAO;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.StudentReport;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ReportingService {

    private final List<StudentReport> reports = new ArrayList<>();

    public void addReport(String studentId, String status, String actualOutput,
                          String errorMessage, String normalizationMode) {

        StudentReport report = new StudentReport();
        report.setStudentId(studentId);
        report.setActualOutput(actualOutput);
        report.setErrorMessage(errorMessage);

        try {
            report.setStatus(ComparisonStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            report.setStatus(ComparisonStatus.ERROR);
        }

        try {
            report.setNormalizationMode(NormalizationMode.valueOf(normalizationMode));
        } catch (IllegalArgumentException e) {
            report.setNormalizationMode(null);
        }

        report.setTimestamp(Instant.now().toString());

        reports.add(report);
    }

    public List<StudentReport> getReports() {
        return reports;
    }

    public void saveToProject(EvaluationResultDAO dao) throws SQLException {
        dao.insertAll(reports);
    }
}
