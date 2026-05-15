package com.ce316.iae.service;

public interface ReportingService {
    void addReport(String studentId, String status, String actualOutput,
                   String errorMessage, String normalizationMode);
}
