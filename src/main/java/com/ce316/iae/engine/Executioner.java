package com.ce316.iae.engine;

import com.ce316.iae.model.ComparisonResult;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.Submission;
import com.ce316.iae.service.ComparisonService;
import com.ce316.iae.service.ReportingService;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public final class Executioner {

    private final ComparisonService comparisonService;
    private final ReportingService reportingService;
    private final Consumer<String> log;

    public Executioner(ComparisonService comparisonService,
                       ReportingService reportingService,
                       Consumer<String> log) {
        this.comparisonService = comparisonService;
        this.reportingService = reportingService;
        this.log = log != null ? log : s -> { };
    }

    public Executioner(ComparisonService comparisonService,
                       ReportingService reportingService) {
        this(comparisonService, reportingService, null);
    }

    public void executeAll(LanguageConfig config,
                           List<Submission> submissions,
                           Path expectedOutputPath,
                           List<String> projectRunArgs,
                           int compileTimeoutSec,
                           int runTimeoutSec,
                           NormalizationMode normalizationMode) {

        NormalizationMode norm = normalizationMode != null ? normalizationMode : NormalizationMode.STRICT;
        log.accept("Execution engine started — students: " + submissions.size());

        for (Submission sub : submissions) {
            log.accept("Processing student " + sub.getStudentId());
            try {
                processSingle(sub, config, expectedOutputPath, projectRunArgs,
                        compileTimeoutSec, runTimeoutSec, norm);
            } catch (Exception e) {
                log.accept("[ERROR] " + sub.getStudentId() + ": " + e.getMessage());
                reportingService.addReport(sub, ComparisonStatus.ERROR, "",
                        "", List.of(), e.getMessage(), norm);
            }
        }

        log.accept("Execution finished.");
    }

    private void processSingle(Submission sub,
                               LanguageConfig config,
                               Path expectedOutputPath,
                               List<String> projectRunArgs,
                               int compileTimeoutSec,
                               int runTimeoutSec,
                               NormalizationMode normMode) {

        Enforcer enforcer = new Enforcer();

        if (config.hasCompileStep()) {
            List<String> compileCmd = config.buildCompileCommand(
                    sub.getMainSourceFile(),
                    sub.getExtractedFolderPath());

            log.accept("  Compiling: " + String.join(" ", compileCmd));
            enforcer.execute(compileCmd, sub.getExtractedFolderPath(), compileTimeoutSec);

            if (enforcer.getExitCode() == -1 && !enforcer.didTimeout()) {
                String msg = buildMissingToolMessage(config.getCompilerPath(), enforcer.getError());
                log.accept("  [ERROR] " + msg);
                reportingService.addReport(sub, ComparisonStatus.ERROR, "", "",
                        List.of(), msg, normMode);
                return;
            }

            if (enforcer.didTimeout()) {
                log.accept("  [TIMEOUT] Compilation timed out.");
                reportingService.addReport(sub, ComparisonStatus.TIMEOUT, "", "",
                        List.of(), "Compilation timed out.", normMode);
                return;
            }

            if (enforcer.getExitCode() != 0) {
                log.accept("  [COMPILE_ERROR] " + enforcer.getError().trim());
                reportingService.addReport(sub, ComparisonStatus.COMPILE_ERROR, "",
                        "", List.of(), enforcer.getError(), normMode);
                return;
            }
            log.accept("  Compilation OK.");
        } else {
            log.accept("  Skipping compilation (interpreted configuration).");
        }

        List<String> runCmd = config.buildRunCommand(
                sub.getExtractedFolderPath(),
                projectRunArgs,
                sub.getMainSourceFile());

        log.accept("  Running: " + String.join(" ", runCmd));
        enforcer.execute(runCmd, sub.getExtractedFolderPath(), runTimeoutSec);

        if (enforcer.getExitCode() == -1 && !enforcer.didTimeout()) {
            String toolName = runCmd.isEmpty() ? "?" : runCmd.get(0);
            String msg = buildMissingToolMessage(toolName, enforcer.getError());
            log.accept("  [ERROR] " + msg);
            reportingService.addReport(sub, ComparisonStatus.ERROR, "", "",
                    List.of(), msg, normMode);
            return;
        }

        if (enforcer.didTimeout()) {
            log.accept("  [TIMEOUT] Execution timed out.");
            reportingService.addReport(sub, ComparisonStatus.TIMEOUT, "",
                    "", List.of(), "Execution timed out.", normMode);
            return;
        }

        if (enforcer.getExitCode() != 0) {
            String crashMsg = "Program exited with code " + enforcer.getExitCode()
                    + (enforcer.getError().isBlank() ? "" : (" — " + enforcer.getError().trim()));
            log.accept("  [FAIL] " + crashMsg);
            reportingService.addReport(sub, ComparisonStatus.FAIL,
                    enforcer.getOutput(), "", List.of(), crashMsg, normMode);
            return;
        }

        String actualOutput = enforcer.getOutput();
        ComparisonResult result = comparisonService.compare(actualOutput, expectedOutputPath, normMode);

        String stderrNote = enforcer.getError().trim();
        String messageForReport = stderrNote.isEmpty() ? "" : stderrNote;

        reportingService.addReport(
                sub,
                result.getStatus(),
                actualOutput,
                result.getExpectedSnapshot(),
                result.getDiffLines(),
                messageForReport,
                normMode);

        log.accept("  Result: " + result.getStatus());
    }

    private String buildMissingToolMessage(String toolName, String rawError) {
        String tool = toolName;
        if (tool.contains("/") || tool.contains("\\")) {
            tool = tool.substring(Math.max(tool.lastIndexOf('/'), tool.lastIndexOf('\\')) + 1);
        }

        String installHint;
        switch (tool.toLowerCase()) {
            case "gcc":
                installHint = "Install GCC (Linux: sudo apt install gcc; Windows: MinGW-w64).";
                break;
            case "g++":
                installHint = "Install G++ (Linux: sudo apt install g++; Windows: MinGW-w64).";
                break;
            case "javac":
            case "java":
                installHint = "Install a JDK (e.g. Temurin 17+) and ensure it is on PATH.";
                break;
            case "python":
            case "python3":
                installHint = "Install Python from python.org or your OS package manager.";
                break;
            default:
                installHint = "Ensure '" + tool + "' exists on PATH or provide an absolute path.";
        }

        return "'" + tool + "' could not be launched. " + installHint
                + "\n[System message: " + rawError + "]";
    }

    public boolean compile(Submission sub, LanguageConfig config, int compileTimeoutSec) {
        if (!config.hasCompileStep()) {
            return true;
        }
        Enforcer enforcer = new Enforcer();
        List<String> cmd = config.buildCompileCommand(sub.getMainSourceFile(), sub.getExtractedFolderPath());
        enforcer.execute(cmd, sub.getExtractedFolderPath(), compileTimeoutSec);
        return !enforcer.didTimeout() && enforcer.getExitCode() == 0;
    }

    public Executed run(Submission sub, LanguageConfig config, List<String> projectRunArgs, int runTimeoutSec) {
        Enforcer enforcer = new Enforcer();
        List<String> cmd = config.buildRunCommand(sub.getExtractedFolderPath(), projectRunArgs, sub.getMainSourceFile());
        enforcer.execute(cmd, sub.getExtractedFolderPath(), runTimeoutSec);

        Executed result = new Executed();
        result.compSuccess = true;
        result.runSuccess = (enforcer.getExitCode() == 0);
        result.output = enforcer.getOutput();
        result.errMessage = enforcer.getError();
        result.timedOut = enforcer.didTimeout();
        return result;
    }
}
