package com.ce316.iae.service;

public interface ComparisonService {
    String compare(String actualOutput, String expectedOutputPath, String normalizationMode);
}
