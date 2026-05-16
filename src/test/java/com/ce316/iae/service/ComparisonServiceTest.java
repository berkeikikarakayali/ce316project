package com.ce316.iae.service;

import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.NormalizationMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonServiceTest {

    private final ComparisonService svc = new ComparisonService();

    @Test
    void pass_strict_match(@TempDir Path tmp) throws Exception {
        Path expected = tmp.resolve("exp.txt");
        Files.writeString(expected, "hello\nworld\r\n", StandardCharsets.UTF_8);
        ComparisonStatus status = svc.compare("hello\nworld\n", expected, NormalizationMode.STRICT).getStatus();
        assertEquals(ComparisonStatus.PASS, status);
    }

    @Test
    void fail_reports_diff(@TempDir Path tmp) throws Exception {
        Path expected = tmp.resolve("exp.txt");
        Files.writeString(expected, "a\nb\n", StandardCharsets.UTF_8);
        var result = svc.compare("a\nc\n", expected, NormalizationMode.STRICT);
        assertEquals(ComparisonStatus.FAIL, result.getStatus());
        assertFalse(result.getDiffLines().isEmpty());
    }

    @Test
    void trim_whitespace_mode(@TempDir Path tmp) throws Exception {
        Path expected = tmp.resolve("exp.txt");
        Files.writeString(expected, " hi \n", StandardCharsets.UTF_8);
        ComparisonStatus status = svc.compare("hi\n", expected, NormalizationMode.TRIM_WHITESPACE).getStatus();
        assertEquals(ComparisonStatus.PASS, status);
    }

    @Test
    void missing_expected_file_errors() {
        ComparisonStatus status = svc.compare("42", Path.of("/no/such/file.txt"), NormalizationMode.STRICT).getStatus();
        assertEquals(ComparisonStatus.ERROR, status);
    }
}
