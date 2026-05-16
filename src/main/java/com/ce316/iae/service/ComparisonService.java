package com.ce316.iae.service;

import com.ce316.iae.model.ComparisonResult;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.NormalizationMode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class ComparisonService {

    public ComparisonResult compare(String actualOutput,
                                    Path expectedOutputPath,
                                    NormalizationMode mode) {
        NormalizationMode m = mode != null ? mode : NormalizationMode.STRICT;
        if (expectedOutputPath == null || !Files.isRegularFile(expectedOutputPath)) {
            List<String> diff = List.of("Expected output file not found: " + expectedOutputPath);
            return new ComparisonResult(ComparisonStatus.ERROR, "", diff);
        }
        try {
            String expected = Files.readString(expectedOutputPath, StandardCharsets.UTF_8);
            String normActual = normalize(actualOutput == null ? "" : actualOutput, m);
            String normExpected = normalize(expected, m);
            if (normActual.equals(normExpected)) {
                return new ComparisonResult(ComparisonStatus.PASS, expected, List.of());
            }
            return new ComparisonResult(ComparisonStatus.FAIL, expected, computeDiff(normActual, normExpected));
        } catch (IOException e) {
            List<String> diff = List.of("Could not read expected output file: " + e.getMessage());
            return new ComparisonResult(ComparisonStatus.ERROR, "", diff);
        }
    }

    static String normalize(String text, NormalizationMode mode) {
        String unified = unifyNewlines(text);
        switch (mode) {
            case STRICT:
                return unified;
            case TRIM_WHITESPACE:
                return trimLines(unified);
            case CASE_INSENSITIVE:
                return trimLines(unified).toLowerCase();
            default:
                return unified;
        }
    }

    private static String unifyNewlines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String trimLines(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i].trim());
        }
        return sb.toString();
    }

    static List<String> computeDiff(String normalizedActual, String normalizedExpected) {
        List<String> la = Arrays.asList(normalizedActual.split("\n", -1));
        List<String> lb = Arrays.asList(normalizedExpected.split("\n", -1));
        int n = Math.max(la.size(), lb.size());
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String a = i < la.size() ? la.get(i) : "<EOF>";
            String b = i < lb.size() ? lb.get(i) : "<EOF>";
            if (!Objects.equals(a, b)) {
                out.add("Line " + (i + 1) + ": expected '" + b + "' vs actual '" + a + "'");
            }
            if (out.size() >= 200) {
                out.add("…diff truncated after 200 differing lines…");
                break;
            }
        }
        return out;
    }
}
