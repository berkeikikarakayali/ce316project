package com.ce316.iae.service;

import com.ce316.iae.dao.ConfigurationDAO;
import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.ValidationResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationService {
    private final ConfigurationDAO configurationDAO;
    private final Map<String, LanguageConfig> configsByLanguage = new LinkedHashMap<>();
    private String activeLanguage;

    public ConfigurationService() {
        this(null);
    }

    public ConfigurationService(ConfigurationDAO configurationDAO) {
        this.configurationDAO = configurationDAO;
    }

    public LanguageConfig getConfigForLanguage(String name) {
        if (name == null) {
            return null;
        }
        LanguageConfig config = configsByLanguage.get(name);
        return config != null ? copyOf(config) : null;
    }

    public LanguageConfig getConfig() {
        if (activeLanguage != null && configsByLanguage.containsKey(activeLanguage)) {
            return copyOf(configsByLanguage.get(activeLanguage));
        }
        if (configsByLanguage.size() == 1) {
            return copyOf(configsByLanguage.values().iterator().next());
        }
        if (configsByLanguage.isEmpty()) {
            throw new IllegalStateException("No language configuration is available");
        }
        throw new IllegalStateException("Active language is not set");
    }

    public void setActiveLanguage(String name) {
        if (name == null || name.trim().isEmpty()) {
            activeLanguage = null;
            return;
        }
        if (!configsByLanguage.containsKey(name)) {
            throw new IllegalArgumentException("Unknown language configuration: " + name);
        }
        activeLanguage = name;
    }

    public void addConfig(LanguageConfig config) {
        LanguageConfig copy = validateForMutation(config);
        if (configsByLanguage.containsKey(copy.getName())) {
            throw new IllegalArgumentException("Configuration already exists: " + copy.getName());
        }
        configsByLanguage.put(copy.getName(), copy);
        if (activeLanguage == null && configsByLanguage.size() == 1) {
            activeLanguage = copy.getName();
        }
    }

    public void updateConfig(LanguageConfig config) {
        LanguageConfig copy = validateForMutation(config);
        if (!configsByLanguage.containsKey(copy.getName())) {
            throw new IllegalArgumentException("Configuration does not exist: " + copy.getName());
        }
        configsByLanguage.put(copy.getName(), copy);
    }

    public void removeConfig(String name) {
        if (name == null) {
            return;
        }
        configsByLanguage.remove(name);
        if (name.equals(activeLanguage)) {
            activeLanguage = null;
        }
    }

    public List<LanguageConfig> listAll() {
        List<LanguageConfig> out = new ArrayList<>();
        for (LanguageConfig config : configsByLanguage.values()) {
            out.add(copyOf(config));
        }
        return out;
    }

    public void loadFromProject() throws SQLException {
        loadFromProject(requireDAO());
    }

    public void loadFromProject(ConfigurationDAO dao) throws SQLException {
        configsByLanguage.clear();
        activeLanguage = null;
        for (LanguageConfig config : dao.findAll()) {
            config.validate();
            configsByLanguage.put(config.getName(), copyOf(config));
        }
        if (configsByLanguage.size() == 1) {
            activeLanguage = configsByLanguage.keySet().iterator().next();
        }
    }

    public void saveToProject() throws SQLException {
        saveToProject(requireDAO());
    }

    public void saveToProject(ConfigurationDAO dao) throws SQLException {
        Map<String, LanguageConfig> existingByLanguage = new LinkedHashMap<>();
        for (LanguageConfig existing : dao.findAll()) {
            existingByLanguage.put(existing.getName(), existing);
        }

        for (String existingName : existingByLanguage.keySet()) {
            if (!configsByLanguage.containsKey(existingName)) {
                dao.delete(existingName);
            }
        }

        for (LanguageConfig current : configsByLanguage.values()) {
            LanguageConfig existing = existingByLanguage.get(current.getName());
            if (existing == null) {
                dao.insert(current);
            } else {
                if (current.getId() == null) {
                    current.setId(existing.getId());
                }
                dao.update(current);
            }
        }
    }

    void replaceWithImported(List<LanguageConfig> importedConfigs) {
        configsByLanguage.clear();
        activeLanguage = null;
        for (LanguageConfig config : importedConfigs) {
            LanguageConfig copy = copyOf(config);
            configsByLanguage.put(copy.getName(), copy);
        }
        if (configsByLanguage.size() == 1) {
            activeLanguage = configsByLanguage.keySet().iterator().next();
        }
    }

    void mergeImported(List<LanguageConfig> importedConfigs) {
        for (LanguageConfig config : importedConfigs) {
            LanguageConfig copy = copyOf(config);
            configsByLanguage.put(copy.getName(), copy);
        }
        if (activeLanguage == null && configsByLanguage.size() == 1) {
            activeLanguage = configsByLanguage.keySet().iterator().next();
        }
    }

    private LanguageConfig validateForMutation(LanguageConfig config) {
        LanguageConfig copy = copyOf(config);
        ValidationResult result = copy.validate();
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getMessage());
        }
        return copy;
    }

    private ConfigurationDAO requireDAO() {
        if (configurationDAO == null) {
            throw new IllegalStateException("No ConfigurationDAO was provided");
        }
        return configurationDAO;
    }

    private static LanguageConfig copyOf(LanguageConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
        LanguageConfig copy = new LanguageConfig(
                config.getName(),
                config.getFileExtension(),
                config.getCompilerPath(),
                config.getCompileArgs(),
                config.getRunArgs());
        copy.setId(config.getId());
        copy.setStatus(config.getStatus());
        return copy;
    }
}
