package com.ce316.iae.service;

import com.ce316.iae.dao.ConfigurationDAO;
import com.ce316.iae.model.LanguageConfig;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigurationService {

    private final ConfigurationDAO dao;
    private final Map<Integer, LanguageConfig> byId = new LinkedHashMap<>();

    public ConfigurationService(ConfigurationDAO dao) {
        this.dao = dao;
    }

    public void reloadFromDb() throws SQLException {
        byId.clear();
        for (LanguageConfig c : dao.findAll()) {
            byId.put(c.getId(), c);
        }
    }

    public LanguageConfig findById(Integer id) {
        return id == null ? null : byId.get(id);
    }

    public List<LanguageConfig> listAll() {
        return new ArrayList<>(byId.values());
    }

    public LanguageConfig findByLanguage(String languageName) throws SQLException {
        return dao.findByLanguage(languageName);
    }

    /**
     * Insert when unknown language name; otherwise replace row by language name (preserving PK).
     */
    public void upsert(LanguageConfig config) throws SQLException {
        LanguageConfig existing = dao.findByLanguage(config.getName());
        if (existing != null) {
            config.setId(existing.getId());
            dao.update(config);
        } else {
            dao.insert(config);
        }
        reloadFromDb();
    }

    public void delete(String languageName) throws SQLException {
        dao.delete(languageName);
        reloadFromDb();
    }
}
