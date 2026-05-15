package com.ce316.iae.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ComparisonService {

    public String compare(String actualOutput, String expectedOutputPath, String normalizationMode) {
        String expectedContent;
        try {
            expectedContent = Files.readString(Paths.get(expectedOutputPath));
        } catch (IOException e) {
            return "ERROR";
        }

        if (actualOutput.equals(expectedContent)) {
            return "PASS";
        } else {
            return "FAIL";
        }
    }
}
