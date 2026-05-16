package com.ce316.iae.service;

import com.ce316.iae.dao.ConfigurationDAO;
import com.ce316.iae.model.ImportMode;
import com.ce316.iae.model.ImportResult;
import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.SkippedEntry;
import com.ce316.iae.model.ValidationResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ImportExportService {

    private static final Type LIST_TYPE = new TypeToken<List<LanguageConfigDto>>() { }.getType();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Gson DTO mirrors exported JSON keys from design §3.4. */
    @SuppressWarnings("unused")
    private static final class LanguageConfigDto {
        String language_name;
        String file_extension;
        String compiler_path;
        List<String> compile_args = List.of();
        List<String> run_args = List.of();
    }

    public void exportToFile(ConfigurationDAO dao, Path path) throws IOException, SQLException {
        List<LanguageConfigDto> dtos = new ArrayList<>();
        for (LanguageConfig c : dao.findAll()) {
            LanguageConfigDto d = new LanguageConfigDto();
            d.language_name = c.getName();
            d.file_extension = c.getFileExtension();
            d.compiler_path = c.getCompilerPath();
            d.compile_args = c.getCompileArgs();
            d.run_args = c.getRunArgs();
            dtos.add(d);
        }
        Files.writeString(path, gson.toJson(dtos), StandardCharsets.UTF_8);
    }

    public ImportResult importFromFile(ConfigurationService configurationService,
                                       Path path,
                                       ImportMode mode) {
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            List<LanguageConfigDto> dtos = gson.fromJson(json, LIST_TYPE);
            if (dtos == null) {
                return ImportResult.failure("File contained no configurations.");
            }

            if (mode == ImportMode.REPLACE) {
                List<String> names = new ArrayList<>();
                for (LanguageConfig c : configurationService.listAll()) {
                    names.add(c.getName());
                }
                for (String n : names) {
                    configurationService.delete(n);
                }
            }

            List<String> importedNames = new ArrayList<>();
            List<SkippedEntry> skipped = new ArrayList<>();

            for (LanguageConfigDto d : dtos) {
                LanguageConfig cfg = new LanguageConfig(
                        d.language_name,
                        d.file_extension,
                        d.compiler_path,
                        d.compile_args != null ? d.compile_args : List.of(),
                        d.run_args != null ? d.run_args : List.of());
                ValidationResult vr = cfg.validate();
                if (!vr.isValid()) {
                    skipped.add(new SkippedEntry(cfg, vr.getMessage()));
                    continue;
                }
                configurationService.upsert(cfg);
                importedNames.add(cfg.getName());
            }

            List<LanguageConfig> importedConfigs = new ArrayList<>();
            for (String name : importedNames) {
                LanguageConfig fresh = configurationService.findByLanguage(name);
                if (fresh != null) {
                    importedConfigs.add(fresh);
                }
            }

            return new ImportResult(true, "", importedConfigs, skipped);
        } catch (IOException e) {
            return ImportResult.failure("Could not read import file: " + e.getMessage());
        } catch (SQLException e) {
            return ImportResult.failure("Database error during import: " + e.getMessage());
        } catch (RuntimeException e) {
            return ImportResult.failure("Invalid configuration JSON: " + e.getMessage());
        }
    }
}
