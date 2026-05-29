package com.ce316.iae.dao;

import com.ce316.iae.model.Submission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StudentSubmissionDAO {

    private final Connection connection;

    public StudentSubmissionDAO(Connection connection) {
        this.connection = connection;
    }

    public void insertAll(List<Submission> submissions) throws SQLException {
        String sql = "INSERT INTO STUDENT_SUBMISSION(student_id, zip_file_path, " +
                     "extracted_folder_path, main_source_file) VALUES(?, ?, ?, ?)";
        boolean prevAuto = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Submission s : submissions) {
                ps.setString(1, s.getStudentId());
                ps.setString(2, s.getZipFilePath());
                ps.setString(3, s.getExtractedFolderPath());
                ps.setString(4, s.getMainSourceFile());
                ps.executeUpdate();

                //test
                System.out.println("Inserted submission: " + s.getStudentId());

/*
commented for test
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        s.setId(keys.getInt(1));
                    }
                }
//fix test1 failed
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        System.out.println("generated id = " + id);
                        s.setId(id);
                    } else {
                        System.out.println("no generated id!");
                    }
                }
 */
                //fix test2 success
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {

                    if (rs.next()) {
                        int id = rs.getInt(1);
                        System.out.println("GENERATED ID = " + id);
                        s.setId(id);
                    }
                }


            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(prevAuto);
        }
    }

    public List<Submission> findAll() throws SQLException {
        String sql = "SELECT id, student_id, zip_file_path, extracted_folder_path, main_source_file " +
                     "FROM STUDENT_SUBMISSION ORDER BY student_id";
        List<Submission> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Submission s = new Submission();
                s.setId(rs.getInt("id"));
                s.setStudentId(rs.getString("student_id"));
                s.setZipFilePath(rs.getString("zip_file_path"));
                s.setExtractedFolderPath(rs.getString("extracted_folder_path"));
                s.setMainSourceFile(rs.getString("main_source_file"));
                out.add(s);
            }
        }
        return out;
    }

    public void deleteAll() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM STUDENT_SUBMISSION");
        }
    }
}
