package com.ce316.iae.engine;

import com.ce316.iae.model.ComparisonResult;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.Submission;
import com.ce316.iae.service.ComparisonService;
import com.ce316.iae.service.ConfigurationService;
import com.ce316.iae.service.ReportingService;

import java.util.List;

/*
**************************************************************************************
IMPORTANT NOTE
all console lines are for developer it self. this engine does not engage with GUI
**************************************************************************************
*/

// this class is main coordinator of ExecutionEngine

/*
-executeAll()-> process students one by one
-gets compile command from config
-enforcer deals with the process:
    -if compiler is not set up => error
    -timeout
    -compile error
-with enforcer it runs:
    -runtime is not set up => error
    -timeout
    -crash issues
    -pass/fail (normal run)
-give results to ReportingService (berke)
 */
public class Executioner {

    private final ConfigurationService configService;
    private final ComparisonService    comparisonService;
    private final ReportingService     reportingService;

    public Executioner(ConfigurationService configService,
                       ComparisonService    comparisonService,
                       ReportingService     reportingService) {
        this.configService     = configService;
        this.comparisonService = comparisonService;
        this.reportingService  = reportingService;
    }
    public void executeAll(List<Submission> submissions,
                           String expectedOutput,
                           String runArgs,
                           int    compileTimeout,
                           int    runTimeout,
                           String normMode) {

        // (dev log) System.out.println("execution engine started: " + submissions.size() + " student(s)\n");

        for (Submission sub : submissions) {
            // (dev log) System.out.println("Processing: " + sub.getStudentId());
            try {
                processSingle(sub, expectedOutput, runArgs, compileTimeout, runTimeout, normMode);
            } catch (Exception e) {
                // (dev log) System.err.println("[ERROR] " + sub.getStudentId() + ": " + e.getMessage());
                reportingService.addReport(sub.getId(), sub.getStudentId(), "ERROR", "", e.getMessage(), normMode);
            }
        }

        // (dev log) System.out.println("\nExecution Engine finished");
    }

    // proccessng single student
    private void processSingle(Submission sub,
                               String expectedOutput,
                               String runArgs,
                               int compileTimeout,
                               int runTimeout,
                               String normMode) {

        LanguageConfig config = configService.getConfig();
        Enforcer enforcer = new Enforcer();

        // compiling
        if (config.hasCompileStep()) {
            List<String> compileCmd = config.buildCompileCommand(
                    sub.getMainSourceFile(),
                    sub.getExtractedFolderPath()
            );

            // (dev log) System.out.println("Compiling: " + String.join(" ", compileCmd));
            enforcer.execute(compileCmd, sub.getExtractedFolderPath(), compileTimeout);

            if (enforcer.getExitCode() == -1 && !enforcer.didTimeout()) {
                String msg = buildMissingToolMessage(config.getCompilerPath(), enforcer.getError());
                // (dev log) System.out.println("err" + msg);
                reportingService.addReport(sub.getId(), sub.getStudentId(), "ERROR", "", msg, normMode);
                return;
            }

            if (enforcer.didTimeout()) {
                // (dev log) System.out.println("Compilation timed out.");
                reportingService.addReport(sub.getId(), sub.getStudentId(), "TIMEOUT", "", "Compilation timed out.", normMode);
                return;
            }

            if (enforcer.getExitCode() != 0) {
                // (dev log) System.out.println("compile err " + enforcer.getError().trim());
                reportingService.addReport(sub.getId(), sub.getStudentId(), "COMPILE_ERROR", "", enforcer.getError(), normMode);
                return;
            }

            // (dev log) System.out.println("Compilation successful.");
        } else {
            // (dev log) System.out.println("No compilation step (interpreted language).");
        }

        //running
        List<String> runCmd = config.buildRunCommand(
                sub.getMainSourceFile(),
                sub.getExtractedFolderPath(),
                runArgs
        );

        // (dev log) System.out.println("Running: " + String.join(" ", runCmd));
        enforcer.execute(runCmd, sub.getExtractedFolderPath(), runTimeout);

        if (enforcer.getExitCode() == -1 && !enforcer.didTimeout()) {
            String toolName = runCmd.isEmpty() ? "?" : runCmd.get(0);
            String msg = buildMissingToolMessage(toolName, enforcer.getError());
            // (dev log) System.out.println("err " + msg);
            reportingService.addReport(sub.getId(), sub.getStudentId(), "ERROR", "", msg, normMode);
            return;
        }

        if (enforcer.didTimeout()) {
            // (dev log) System.out.println("Execution timed out.");
            reportingService.addReport(sub.getId(), sub.getStudentId(), "TIMEOUT", "", "Execution timed out.", normMode);
            return;
        }

        if (enforcer.getExitCode() != 0) {
            String crashMsg = "Program crashed (exit code " + enforcer.getExitCode() + "). "
                    + enforcer.getError().trim();
            // (dev log) System.out.println("[CRASH→FAIL] " + crashMsg);
            reportingService.addReport(sub.getId(), sub.getStudentId(), "FAIL",
                    enforcer.getOutput(), crashMsg, normMode);
            return;
        }

        // compare
        String actualOutput = enforcer.getOutput();
        // (dev log) System.out.println("Output: " + actualOutput.trim());

        ComparisonResult cmpResult = comparisonService.compare(actualOutput, expectedOutput, normMode);
        // (dev log) System.out.println("Result: " + cmpResult.getStatus().name());

        reportingService.addReport(sub.getId(), sub.getStudentId(), cmpResult, enforcer.getError(), normMode);
    }

    // utility
    private String buildMissingToolMessage(String toolName, String rawError) {
        String tool = toolName;
        if (tool.contains("/") || tool.contains("\\")) {
            tool = tool.substring(Math.max(tool.lastIndexOf('/'), tool.lastIndexOf('\\')) + 1);
        }

        String installHint;
        switch (tool.toLowerCase()) {
            case "gcc":
                installHint = "To install GCC: MinGW (Windows) → mingw-w64.org | Linux → sudo apt install gcc";
                break;
            case "g++":
                installHint = "To install G++: MinGW (Windows) → mingw-w64.org | Linux → sudo apt install g++";
                break;
            case "mcs":
            case "mono":
                installHint = "To install Mono: mono-project.com | Linux → sudo apt install mono-complete";
                break;
            case "javac":
            case "java":
                installHint = "To install Java JDK: adoptium.net | Linux → sudo apt install default-jdk";
                break;
            case "ghc":
                installHint = "To install GHC: haskell.org/ghcup | Linux → sudo apt install ghc";
                break;
            case "python":
            case "python3":
                installHint = "To install Python: python.org | Linux → sudo apt install python3";
                break;
            case "swipl":
                installHint = "To install SWI-Prolog: swi-prolog.org | Linux → sudo apt install swi-prolog";
                break;
            default:
                installHint = "Make sure '" + tool + "' is installed and on the PATH.";
        }

        return "'" + tool + "' not found or could not be started. " + installHint
                + "\n[System error: " + rawError + "]";
    }

    public boolean compile(Submission sub, int compileTimeout) {
        LanguageConfig config = configService.getConfig();
        if (!config.hasCompileStep()) return true;

        Enforcer enforcer = new Enforcer();
        List<String> cmd = config.buildCompileCommand(
                sub.getMainSourceFile(), sub.getExtractedFolderPath());
        enforcer.execute(cmd, sub.getExtractedFolderPath(), compileTimeout);
        return !enforcer.didTimeout() && enforcer.getExitCode() == 0;
    }

    public Executed run(Submission sub, String runArgs, int runTimeout) {
        LanguageConfig config = configService.getConfig();
        Enforcer enforcer = new Enforcer();

        List<String> cmd = config.buildRunCommand(
                sub.getMainSourceFile(), sub.getExtractedFolderPath(), runArgs);
        enforcer.execute(cmd, sub.getExtractedFolderPath(), runTimeout);

        Executed result = new Executed();
        result.compSuccess = true;
        result.runSuccess  = (enforcer.getExitCode() == 0);
        result.output      = enforcer.getOutput();
        result.errMessage  = enforcer.getError();
        result.timedOut    = enforcer.didTimeout();
        return result;
    }
}
