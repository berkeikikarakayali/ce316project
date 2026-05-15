package com.ce316.iae.service;

import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.ValidationResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImportExportService {
    private final ConfigurationService configurationService;

    public ImportExportService(ConfigurationService configurationService) {
        if (configurationService == null) {
            throw new IllegalArgumentException("ConfigurationService must not be null");
        }
        this.configurationService = configurationService;
    }

    public void exportToFile(String path) throws IOException {
        exportToFile(Path.of(path));
    }

    public void exportToFile(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(configurationService.listAll()), StandardCharsets.UTF_8);
    }

    public ImportResult importFromFile(String path) {
        return importFromFile(Path.of(path), ImportMode.MERGE);
    }

    public ImportResult importFromFile(String path, ImportMode mode) {
        return importFromFile(Path.of(path), mode);
    }

    public ImportResult importFromFile(Path path) {
        return importFromFile(path, ImportMode.MERGE);
    }

    public ImportResult importFromFile(Path path, ImportMode mode) {
        if (path == null) {
            return ImportResult.failure("Import path must not be null");
        }
        ImportMode importMode = mode != null ? mode : ImportMode.MERGE;
        List<LanguageConfig> parsed;
        try {
            parsed = parseConfigs(Files.readString(path, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return ImportResult.failure(e.getMessage());
        }

        List<LanguageConfig> imported = new ArrayList<>();
        List<SkippedEntry> skipped = new ArrayList<>();
        for (LanguageConfig config : parsed) {
            ValidationResult result = config.validate();
            if (result.isValid()) {
                imported.add(config);
            } else {
                skipped.add(new SkippedEntry(config, result.getMessage()));
            }
        }

        if (importMode == ImportMode.REPLACE) {
            configurationService.replaceWithImported(imported);
        } else {
            configurationService.mergeImported(imported);
        }
        return new ImportResult(true, imported, skipped, "");
    }

    private static String toJson(List<LanguageConfig> configs) {
        StringBuilder out = new StringBuilder();
        out.append("[\n");
        for (int i = 0; i < configs.size(); i++) {
            LanguageConfig config = configs.get(i);
            out.append("  {\n");
            out.append("    \"name\": ").append(jsonString(config.getName())).append(",\n");
            out.append("    \"fileExtension\": ").append(jsonString(config.getFileExtension())).append(",\n");
            out.append("    \"compilerPath\": ").append(jsonString(config.getCompilerPath())).append(",\n");
            out.append("    \"compileArgs\": ").append(jsonArray(config.getCompileArgs())).append(",\n");
            out.append("    \"runArgs\": ").append(jsonArray(config.getRunArgs())).append("\n");
            out.append("  }");
            if (i + 1 < configs.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("]\n");
        return out.toString();
    }

    private static List<LanguageConfig> parseConfigs(String json) {
        Object root = new JsonParser(json).parse();
        if (!(root instanceof List)) {
            throw new IllegalArgumentException("Configuration import must be a JSON array");
        }

        List<LanguageConfig> configs = new ArrayList<>();
        for (Object item : (List<?>) root) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("Each configuration entry must be an object");
            }
            Map<?, ?> object = (Map<?, ?>) item;
            LanguageConfig config = new LanguageConfig();
            config.setName(asString(object.get("name")));
            config.setFileExtension(asString(object.get("fileExtension")));
            config.setCompilerPath(asString(object.get("compilerPath")));
            config.setCompileArgs(asStringList(object.get("compileArgs")));
            config.setRunArgs(asStringList(object.get("runArgs")));
            configs.add(config);
        }
        return configs;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("Expected a string value in configuration import");
        }
        return (String) value;
    }

    private static List<String> asStringList(Object value) {
        List<String> out = new ArrayList<>();
        if (value == null) {
            return out;
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("Expected an array of strings in configuration import");
        }
        for (Object item : (List<?>) value) {
            if (!(item instanceof String)) {
                throw new IllegalArgumentException("Expected an array of strings in configuration import");
            }
            out.add((String) item);
        }
        return out;
    }

    private static String jsonArray(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(jsonString(values.get(i)));
        }
        out.append(']');
        return out.toString();
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default: out.append(c);
            }
        }
        out.append('"');
        return out.toString();
    }

    private static final class JsonParser {
        private final String input;
        private int pos;

        JsonParser(String input) {
            this.input = input != null ? input : "";
        }

        Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (pos != input.length()) {
                throw new IllegalArgumentException("Unexpected content after JSON value");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= input.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char c = input.charAt(pos);
            if (c == '"') {
                return parseString();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '{') {
                return parseObject();
            }
            if (input.startsWith("null", pos)) {
                pos += 4;
                return null;
            }
            throw new IllegalArgumentException("Unexpected JSON token at position " + pos);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                pos++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    pos++;
                    return object;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                pos++;
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    pos++;
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos++);
                if (c == '"') {
                    return out.toString();
                }
                if (c == '\\') {
                    if (pos >= input.length()) {
                        throw new IllegalArgumentException("Unfinished escape sequence in JSON string");
                    }
                    char escaped = input.charAt(pos++);
                    switch (escaped) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case '/': out.append('/'); break;
                        case 'b': out.append('\b'); break;
                        case 'f': out.append('\f'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'u': out.append(parseUnicodeEscape()); break;
                        default:
                            throw new IllegalArgumentException("Unsupported escape sequence: \\" + escaped);
                    }
                } else {
                    out.append(c);
                }
            }
            throw new IllegalArgumentException("Unterminated JSON string");
        }

        private char parseUnicodeEscape() {
            if (pos + 4 > input.length()) {
                throw new IllegalArgumentException("Invalid unicode escape in JSON string");
            }
            String hex = input.substring(pos, pos + 4);
            pos += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid unicode escape in JSON string", e);
            }
        }

        private void skipWhitespace() {
            while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
        }

        private boolean peek(char expected) {
            return pos < input.length() && input.charAt(pos) == expected;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (pos >= input.length() || input.charAt(pos) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos);
            }
            pos++;
        }
    }
}
