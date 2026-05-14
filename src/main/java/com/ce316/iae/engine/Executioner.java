package com.ce316.iae.engine;

import model.Submission;
import model.LanguageConfig;
import service.ConfigurationService;
import service.ComparisonService;
import service.ReportingService;

import java.util.List;

/*
**************************************************************************************
IMPORTANT NOTE

THIS CLASS HAS SO MANY CONNECTIONS BETWEEN OTHER MODULES SO IT HAS MANY PLACEHOLDERS ERRORS COMING FROM THERE PLEASE IGNORE FOR NOW
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

        System.out.println("Execution Engine started: " + submissions.size() + " student\n");

        for (Submission sub : submissions) {
            System.out.println("Processing: " + sub.studentId );
            try {
                processSingle(sub, expectedOutput, runArgs, compileTimeout, runTimeout, normMode);
            } catch (Exception e) {
                System.err.println("[ERROR] " + sub.studentId + ": " + e.getMessage());
                reportingService.addReport(sub.studentId, "ERROR", "", e.getMessage(), normMode);
            }
        }

        System.out.println("\n=== Execution Engine tamamlandı ===");
    }

    // proccessng single student
    private void processSingle(Submission sub,
                               String expectedOutput,
                               String runArgs,
                               int compileTimeout,
                               int runTimeout,
                               String normMode) {

        LanguageConfig config = configService.getConfig();
        engine.Enforcer enforcer = new engine.Enforcer();

        // compiling
        if (config.hasCompileStep()) {
            List<String> compileCmd = config.buildCompileCommand(
                    sub.mainSourceFile,
                    sub.extractedFolderPath
            );

            System.out.println("  Compiling: " + String.join(" ", compileCmd));
            enforcer.execute(compileCmd, sub.extractedFolderPath, compileTimeout);

            if (enforcer.getExitCode() == -1 && !enforcer.didTimeout()) {
                String msg = buildMissingToolMessage(config.getCompilerPath(), enforcer.getError());
                System.out.println("  [ERROR] " + msg);
                reportingService.addReport(sub.studentId, "ERROR", "", msg, normMode);
                return;
            }

            if (enforcer.didTimeout()) {
                System.out.println("  [TIMEOUT] The compilation has timed out.");
                reportingService.addReport(sub.studentId, "TIMEOUT", "", "Compiling timeout.", normMode);
                return;
            }

            if (enforcer.getExitCode() != 0) {
                System.out.println("  [COMPILE_ERROR] " + enforcer.getError().trim());
                reportingService.addReport(sub.studentId, "COMPILE_ERROR", "", enforcer.getError(), normMode);
                return;
            }

            System.out.println("Compilation successful. ");
        } else {
            System.out.println("  No compilation step (interpreted language).");
        }

        //running
        List<String> runCmd = config.buildRunCommand(
                sub.extractedFolderPath,
                runArgs
        );

        System.out.println("  Running: " + String.join(" ", runCmd));
        enforcer.execute(runCmd, sub.extractedFolderPath, runTimeout);

        if (enforcer.getExitCode() == -1 && !enforcer.didTimeout()) {
            String toolName = runCmd.isEmpty() ? "?" : runCmd.get(0);
            String msg = buildMissingToolMessage(toolName, enforcer.getError());
            System.out.println("  [ERROR] " + msg);
            reportingService.addReport(sub.studentId, "ERROR", "", msg, normMode);
            return;
        }

        if (enforcer.didTimeout()) {
            System.out.println("  [TIMEOUT] The operation timed out. ");
            reportingService.addReport(sub.studentId, "TIMEOUT", "", " Operation timeout.", normMode);
            return;
        }

        if (enforcer.getExitCode() != 0) {
            String crashMsg = "Program crashed (exit code " + enforcer.getExitCode() + "). "
                    + enforcer.getError().trim();
            System.out.println("  [CRASH→FAIL] " + crashMsg);
            reportingService.addReport(sub.studentId, "FAIL",
                    enforcer.getOutput(), crashMsg, normMode);
            return;
        }

        String actualOutput = enforcer.getOutput();
        System.out.println("  Output: " + actualOutput.trim());

        //comparing
        String status = comparisonService.compare(actualOutput, expectedOutput, normMode);
        System.out.println("  Result: " + status);

        reportingService.addReport(sub.studentId, status, actualOutput, enforcer.getError(), normMode);
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
                installHint = "To install GCC: 'sudo apt install gcc' (Linux) or MinGW (Windows)";
                break;
            case "g++":
                installHint = "To install G++: 'sudo apt install g++' (Linux) or MinGW (Windows)";
                break;
            case "mcs":
            case "mono":
                installHint = "To install Mono: 'sudo apt install mono-complete' (Linux) or mono-project.com (Windows)";
                break;
            case "javac":
            case "java":
                installHint = "To install Java JDK: use adoptium.net or 'sudo apt install default-jdk'";
                break;
            case "ghc":
                installHint = "To install GHC: 'sudo apt install ghc' (Linux) or haskell.org/ghcup (Windows)";
                break;
            case "python":
            case "python3":
                installHint = "To install Python: visit python.org or use the command 'sudo apt install python3'";
                break;
            case "swipl":
                installHint = "To install SWI-Prolog: 'sudo apt install swi-prolog' (Linux) or swi-prolog.org (Windows)";
                break;
            default:
                installHint = "Please '" + tool + "Make sure the vehicle is installed and in the PATH.' ";
        }

        return "'" + tool + "' Not found or could not be run. " + installHint
                + "\n[error: " + rawError + "]";
    }


    public boolean compile(Submission sub, int compileTimeout) {
        LanguageConfig config = configService.getConfig();
        if (!config.hasCompileStep()) return true;

        engine.Enforcer enforcer = new engine.Enforcer();
        List<String> cmd = config.buildCompileCommand(sub.mainSourceFile, sub.extractedFolderPath);
        enforcer.execute(cmd, sub.extractedFolderPath, compileTimeout);
        return !enforcer.didTimeout() && enforcer.getExitCode() == 0;
    }

    public engine.Executed run(Submission sub, String runArgs, int runTimeout) {
        LanguageConfig config = configService.getConfig();
        engine.Enforcer enforcer = new engine.Enforcer();

        List<String> cmd = config.buildRunCommand(sub.extractedFolderPath, runArgs);
        enforcer.execute(cmd, sub.extractedFolderPath, runTimeout);

        engine.Executed result = new engine.Executed();
        result.compSuccess = true;
        result.runSuccess  = (enforcer.getExitCode() == 0);
        result.output      = enforcer.getOutput();
        result.errMessage  = enforcer.getError();
        result.timedOut    = enforcer.didTimeout();
        return result;
    }
}
