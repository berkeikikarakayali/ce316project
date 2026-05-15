package com.ce316.iae.model;

import java.io.File;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LanguageConfig {
    private static final Set<String> PLACEHOLDERS = Set.of(
            "source", "sourceName", "className", "workDir", "executable", "exe");

    private Integer id;
    private String name;
    private String fileExtension;
    private String compilerPath;
    private List<String> compileArgs;
    private List<String> runArgs;
    private ConfigStatus status;

    public LanguageConfig() {
        this.compileArgs = new ArrayList<>();
        this.runArgs = new ArrayList<>();
        this.status = ConfigStatus.INVALID_ARGS;
    }

    public LanguageConfig(String name, String fileExtension, String compilerPath,
                          List<String> compileArgs, List<String> runArgs) {
        this.name = name;
        this.fileExtension = fileExtension;
        this.compilerPath = compilerPath;
        this.compileArgs = compileArgs != null ? new ArrayList<>(compileArgs) : new ArrayList<>();
        this.runArgs = runArgs != null ? new ArrayList<>(runArgs) : new ArrayList<>();
        this.status = ConfigStatus.INVALID_ARGS;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

    public String getCompilerPath() { return compilerPath; }
    public void setCompilerPath(String compilerPath) { this.compilerPath = compilerPath; }

    public List<String> getCompileArgs() { return compileArgs; }
    public void setCompileArgs(List<String> compileArgs) {
        this.compileArgs = compileArgs != null ? new ArrayList<>(compileArgs) : new ArrayList<>();
    }

    public List<String> getRunArgs() { return runArgs; }
    public void setRunArgs(List<String> runArgs) {
        this.runArgs = runArgs != null ? new ArrayList<>(runArgs) : new ArrayList<>();
    }

    public ConfigStatus getStatus() { return status; }
    public void setStatus(ConfigStatus status) {
        this.status = status != null ? status : ConfigStatus.INVALID_ARGS;
    }

    public boolean hasCompileStep() {
        return compileArgs != null && !compileArgs.isEmpty();
    }

    public ValidationResult validate() {
        if (isBlank(name)) {
            return fail(ConfigStatus.INVALID_ARGS, "Language name is required");
        }
        if (isBlank(fileExtension)) {
            return fail(ConfigStatus.INVALID_ARGS, "File extension is required");
        }

        String compileArgError = validateArgs("compileArgs", compileArgs);
        if (compileArgError != null) {
            return fail(ConfigStatus.INVALID_ARGS, compileArgError);
        }
        String runArgError = validateArgs("runArgs", runArgs);
        if (runArgError != null) {
            return fail(ConfigStatus.INVALID_ARGS, runArgError);
        }

        if (hasCompileStep()) {
            ValidationResult compilerResult = validateTool(compilerPath, "Compiler");
            if (!compilerResult.isValid()) {
                return compilerResult;
            }
        }

        if (runArgs != null && !runArgs.isEmpty() && !containsPlaceholder(runArgs.get(0))) {
            ValidationResult runtimeResult = validateTool(runArgs.get(0), "Runtime command");
            if (!runtimeResult.isValid()) {
                return runtimeResult;
            }
        }

        status = ConfigStatus.VALID;
        return new ValidationResult(ConfigStatus.VALID, "");
    }

    public List<String> buildCompileCommand(String sourceFile, String workingDir) {
        List<String> command = new ArrayList<>();
        if (!hasCompileStep()) {
            return command;
        }
        CommandContext context = new CommandContext(sourceFile, workingDir);
        command.add(expand(compilerPath, context));
        command.addAll(expandArgs(compileArgs, context));
        return command;
    }

    public List<String> buildRunCommand(String sourceFile, String workingDir, String projectRunArgs) {
        CommandContext context = new CommandContext(sourceFile, workingDir);
        List<String> command = expandArgs(runArgs, context);
        command.addAll(splitCommandLine(projectRunArgs));
        return command;
    }

    public List<String> buildRunCommand(String workingDir, String projectRunArgs) {
        return buildRunCommand(null, workingDir, projectRunArgs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LanguageConfig)) return false;
        LanguageConfig that = (LanguageConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }

    private ValidationResult validateTool(String command, String label) {
        if (isBlank(command)) {
            return fail(ConfigStatus.INVALID_PATH, label + " path is required");
        }
        if (containsPlaceholder(command)) {
            return new ValidationResult(ConfigStatus.VALID, "");
        }
        if (isExplicitPath(command)) {
            try {
                if (Files.isRegularFile(Path.of(command))) {
                    return new ValidationResult(ConfigStatus.VALID, "");
                }
            } catch (InvalidPathException e) {
                return fail(ConfigStatus.INVALID_PATH, label + " path is invalid: " + command);
            }
            return fail(ConfigStatus.INVALID_PATH, label + " not found at " + command);
        }
        if (isOnPath(command)) {
            return new ValidationResult(ConfigStatus.VALID, "");
        }
        return fail(ConfigStatus.INVALID_PATH, label + " not found on PATH: " + command);
    }

    private ValidationResult fail(ConfigStatus failedStatus, String message) {
        status = failedStatus;
        return new ValidationResult(failedStatus, message);
    }

    private static String validateArgs(String fieldName, List<String> args) {
        if (args == null) {
            return fieldName + " must not be null";
        }
        for (String arg : args) {
            if (arg == null) {
                return fieldName + " must not contain null values";
            }
            if (arg.trim().isEmpty()) {
                return fieldName + " must not contain blank values";
            }
            String placeholderError = validatePlaceholders(arg);
            if (placeholderError != null) {
                return placeholderError;
            }
        }
        return null;
    }

    private static String validatePlaceholders(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '{') {
                int end = value.indexOf('}', i + 1);
                if (end < 0) {
                    return "Unclosed placeholder in argument: " + value;
                }
                String placeholder = value.substring(i + 1, end);
                if (!PLACEHOLDERS.contains(placeholder)) {
                    return "Unknown placeholder: {" + placeholder + "}";
                }
                i = end;
            } else if (c == '}') {
                return "Unexpected '}' in argument: " + value;
            }
        }
        return null;
    }

    private static boolean containsPlaceholder(String value) {
        return value != null && value.indexOf('{') >= 0 && value.indexOf('}') > value.indexOf('{');
    }

    private static List<String> expandArgs(List<String> args, CommandContext context) {
        List<String> out = new ArrayList<>();
        if (args == null) {
            return out;
        }
        for (String arg : args) {
            out.add(expand(arg, context));
        }
        return out;
    }

    private static String expand(String value, CommandContext context) {
        if (value == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '{') {
                int end = value.indexOf('}', i + 1);
                if (end < 0) {
                    throw new IllegalArgumentException("Unclosed placeholder in argument: " + value);
                }
                String placeholder = value.substring(i + 1, end);
                out.append(context.valueFor(placeholder));
                i = end;
            } else if (c == '}') {
                throw new IllegalArgumentException("Unexpected '}' in argument: " + value);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static List<String> splitCommandLine(String args) {
        List<String> tokens = new ArrayList<>();
        if (isBlank(args)) {
            return tokens;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;
        boolean escaping = false;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (inQuote) {
                if (c == quoteChar) {
                    inQuote = false;
                } else {
                    current.append(c);
                }
                continue;
            }
            if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
            } else if (Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (escaping) {
            current.append('\\');
        }
        if (inQuote) {
            throw new IllegalArgumentException("Unbalanced quotes in project run arguments");
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static boolean isExplicitPath(String command) {
        return command.contains("/") || command.contains("\\") || command.startsWith(".");
    }

    private static boolean isOnPath(String command) {
        String path = System.getenv("PATH");
        if (isBlank(path)) {
            return false;
        }
        String[] suffixes = executableSuffixes();
        for (String dir : path.split(File.pathSeparator)) {
            if (isBlank(dir)) {
                continue;
            }
            for (String suffix : suffixes) {
                try {
                    if (Files.isRegularFile(Path.of(dir, command + suffix))) {
                        return true;
                    }
                } catch (InvalidPathException ignored) {
                    // Ignore invalid PATH entries.
                }
            }
        }
        return false;
    }

    private static String[] executableSuffixes() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return new String[] { "" };
        }
        String pathext = System.getenv("PATHEXT");
        if (isBlank(pathext)) {
            return new String[] { "", ".exe", ".bat", ".cmd" };
        }
        String[] parts = pathext.split(";");
        String[] suffixes = new String[parts.length + 1];
        suffixes[0] = "";
        System.arraycopy(parts, 0, suffixes, 1, parts.length);
        return suffixes;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class CommandContext {
        private final String source;
        private final String sourceName;
        private final String className;
        private final String workDir;
        private final String executable;

        CommandContext(String sourceFile, String workingDir) {
            this.source = sourceFile;
            this.workDir = workingDir;
            this.sourceName = sourceFile != null ? Path.of(sourceFile).getFileName().toString() : null;
            this.className = sourceName != null ? stripExtension(sourceName) : null;
            if (className != null && !isBlank(workingDir)) {
                this.executable = Path.of(workingDir).resolve(className).toString();
            } else {
                this.executable = className;
            }
        }

        String valueFor(String placeholder) {
            switch (placeholder) {
                case "source":
                    return require(source, "{source}");
                case "sourceName":
                    return require(sourceName, "{sourceName}");
                case "className":
                    return require(className, "{className}");
                case "workDir":
                    return require(workDir, "{workDir}");
                case "executable":
                case "exe":
                    return require(executable, "{" + placeholder + "}");
                default:
                    throw new IllegalArgumentException("Unknown placeholder: {" + placeholder + "}");
            }
        }

        private static String require(String value, String placeholder) {
            if (value == null) {
                throw new IllegalArgumentException("Placeholder " + placeholder + " requires a source file or working directory");
            }
            return value;
        }

        private static String stripExtension(String fileName) {
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }
    }
}
