package com.ce316.iae.dao;

import com.ce316.iae.db.JsonArrayCodec;
import com.ce316.iae.model.LanguageConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationDAO {

    private final Connection connection;

    public ConfigurationDAO(Connection connection) {
        this.connection = connection;
    }

    public LanguageConfig findByLanguage(String languageName) throws SQLException {
        String sql = "SELECT id, language_name, file_extension, compiler_path, compile_args, run_args " +
                     "FROM CONFIGURATION WHERE language_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, languageName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readRow(rs) : null;
            }
        }
    }

    public List<LanguageConfig> findAll() throws SQLException {
        String sql = "SELECT id, language_name, file_extension, compiler_path, compile_args, run_args " +
                     "FROM CONFIGURATION ORDER BY language_name";
        List<LanguageConfig> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(readRow(rs));
            }
        }
        return out;
    }

    public int insert(LanguageConfig config) throws SQLException {
        String sql = "INSERT INTO CONFIGURATION(language_name, file_extension, compiler_path, " +
                     "compile_args, run_args) VALUES(?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, config.getName());
            ps.setString(2, config.getFileExtension());
            ps.setString(3, config.getCompilerPath());
            ps.setString(4, JsonArrayCodec.encode(config.getCompileArgs()));
            ps.setString(5, JsonArrayCodec.encode(config.getRunArgs()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    config.setId(id);
                    return id;
                }
            }
        }
        throw new SQLException("Insert did not return a generated id");
    }

    public void update(LanguageConfig config) throws SQLException {
        String sql = "UPDATE CONFIGURATION SET file_extension = ?, compiler_path = ?, " +
                     "compile_args = ?, run_args = ? WHERE language_name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.getFileExtension());
            ps.setString(2, config.getCompilerPath());
            ps.setString(3, JsonArrayCodec.encode(config.getCompileArgs()));
            ps.setString(4, JsonArrayCodec.encode(config.getRunArgs()));
            ps.setString(5, config.getName());
            ps.executeUpdate();
        }
    }

    public void delete(String languageName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM CONFIGURATION WHERE language_name = ?")) {
            ps.setString(1, languageName);
            ps.executeUpdate();
        }
    }

    private LanguageConfig readRow(ResultSet rs) throws SQLException {
        LanguageConfig c = new LanguageConfig();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("language_name"));
        c.setFileExtension(rs.getString("file_extension"));
        c.setCompilerPath(rs.getString("compiler_path"));
        c.setCompileArgs(JsonArrayCodec.decode(rs.getString("compile_args")));
        c.setRunArgs(JsonArrayCodec.decode(rs.getString("run_args")));
        return c;
    }
}
