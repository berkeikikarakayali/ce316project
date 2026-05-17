package com.ce316.iae.service;

import com.ce316.iae.model.ComparisonResult;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.NormalizationMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComparisonService {

    public ComparisonResult compare(String actualOutput, String expectedOutputPath, String normalizationMode) {
        String expectedContent;
        try {
            expectedContent = Files.readString(Paths.get(expectedOutputPath));
        } catch (IOException e) {
            return new ComparisonResult(ComparisonStatus.ERROR, actualOutput, "", new ArrayList<>());
        }

        NormalizationMode mode;
        try {
            mode = NormalizationMode.valueOf(normalizationMode);
        } catch (IllegalArgumentException e) {
            mode = NormalizationMode.STRICT;
        }

        String normalizedActual   = normalize(actualOutput, mode);
        String normalizedExpected = normalize(expectedContent, mode);

        if (normalizedActual.equals(normalizedExpected)) {
            return new ComparisonResult(ComparisonStatus.PASS, actualOutput, expectedContent, new ArrayList<>());
        }

        List<String> diff = computeDiff(
                Arrays.asList(normalizedActual.split("\n", -1)),
                Arrays.asList(normalizedExpected.split("\n", -1))
        );
        return new ComparisonResult(ComparisonStatus.FAIL, actualOutput, expectedContent, diff);
    }

    private String normalize(String text, NormalizationMode mode) {
        // strip UTF-8 BOM
        String result = text.startsWith("\uFEFF") ? text.substring(1) : text;
        result = result.replace("\r\n", "\n");
        if (mode == NormalizationMode.STRICT) {
            return result;
        }
        String[] lines = result.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].strip();
        }
        if (mode == NormalizationMode.CASE_INSENSITIVE) {
            for (int i = 0; i < lines.length; i++) {
                lines[i] = lines[i].toLowerCase();
            }
        }
        return String.join("\n", lines);
    }

    private List<String> computeDiff(List<String> actual, List<String> expected) {
        List<String> diff = new ArrayList<>();
        int maxLen = Math.max(actual.size(), expected.size());
        for (int i = 0; i < maxLen; i++) {
            String a = i < actual.size()   ? actual.get(i)   : "<missing>";
            String e = i < expected.size() ? expected.get(i) : "<missing>";
            if (!a.equals(e)) {
                diff.add("line " + (i + 1) + ": expected [" + e + "] got [" + a + "]");
            }
        }
        return diff;
    }
}
