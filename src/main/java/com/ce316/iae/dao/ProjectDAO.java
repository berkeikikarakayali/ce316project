package com.ce316.iae.dao;

import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class ProjectDAO {

    private final Connection connection;

    public ProjectDAO(Connection connection) {
        this.connection = connection;
    }

    public Project loadProject() throws SQLException {
        String sql = "SELECT name, configuration_id, expected_output_path, zip_folder_path, " +
                     "main_source_filename, run_args, compile_timeout_sec, run_timeout_sec, normalization_mode " +
                     "FROM PROJECT WHERE id = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Project p = new Project();
            p.setName(rs.getString("name"));
            int configId = rs.getInt("configuration_id");
            p.setConfigurationId(rs.wasNull() ? null : configId);
            p.setExpectedOutputPath(rs.getString("expected_output_path"));
            p.setZipFolderPath(rs.getString("zip_folder_path"));
            p.setMainSourceFilename(rs.getString("main_source_filename"));
            p.setRunArgs(rs.getString("run_args"));
            p.setCompileTimeoutSec(rs.getInt("compile_timeout_sec"));
            p.setRunTimeoutSec(rs.getInt("run_timeout_sec"));
            p.setNormalizationMode(NormalizationMode.valueOf(rs.getString("normalization_mode")));
            return p;
        }
    }

    public void updateProject(Project project) throws SQLException {
        String sql = "UPDATE PROJECT SET name = ?, configuration_id = ?, expected_output_path = ?, " +
                     "zip_folder_path = ?, main_source_filename = ?, run_args = ?, " +
                     "compile_timeout_sec = ?, run_timeout_sec = ?, normalization_mode = ? WHERE id = 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, project.getName());
            if (project.getConfigurationId() != null) {
                ps.setInt(2, project.getConfigurationId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setString(3, project.getExpectedOutputPath());
            ps.setString(4, project.getZipFolderPath());
            ps.setString(5, project.getMainSourceFilename());
            ps.setString(6, project.getRunArgs());
            ps.setInt(7, project.getCompileTimeoutSec());
            ps.setInt(8, project.getRunTimeoutSec());
            ps.setString(9, project.getNormalizationMode().name());
            ps.executeUpdate();
        }
    }
}
