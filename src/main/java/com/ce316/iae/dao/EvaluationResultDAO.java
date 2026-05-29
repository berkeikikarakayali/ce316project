package com.ce316.iae.dao;

import com.ce316.iae.db.JsonArrayCodec;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.StudentReport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class EvaluationResultDAO {

    private final Connection connection;

    public EvaluationResultDAO(Connection connection) {
        this.connection = connection;
    }

    public void insertAll(List<StudentReport> reports) throws SQLException {
        String sql = "INSERT INTO EVALUATION_RESULT(student_submission_id, status, actual_output, " +
                     "expected_output, diff_lines, error_message, normalization_mode, timestamp) " +
                     "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        boolean prevAuto = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (StudentReport r : reports) {
                if (r.getStudentSubmissionId() != null) {
                    ps.setInt(1, r.getStudentSubmissionId());
                } else {
                    ps.setNull(1, Types.INTEGER);
                }
                //temp testing delete when extra testing or keep it for memories idk
                System.out.println("STATUS = " + r.getStatus());
                System.out.println("TIMESTAMP = " + r.getTimestamp());
                System.out.println("SUBMISSION ID = " + r.getStudentSubmissionId());

                ps.setString(2, r.getStatus().name());
                ps.setString(3, r.getActualOutput());
                ps.setString(4, r.getExpectedOutput());
                ps.setString(5, JsonArrayCodec.encode(r.getDiffLines()));
                ps.setString(6, r.getErrorMessage());
                ps.setString(7, r.getNormalizationMode() != null ? r.getNormalizationMode().name() : null);
                ps.setString(8, r.getTimestamp());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        r.setId(keys.getInt(1));
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

    public List<StudentReport> findAll() throws SQLException {
        String sql = "SELECT er.id, er.student_submission_id, ss.student_id, er.status, er.actual_output, " +
                     "er.expected_output, er.diff_lines, er.error_message, er.normalization_mode, er.timestamp " +
                     "FROM EVALUATION_RESULT er " +
                     "LEFT JOIN STUDENT_SUBMISSION ss ON ss.id = er.student_submission_id " +
                     "ORDER BY er.id";
        List<StudentReport> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StudentReport r = new StudentReport();
                r.setId(rs.getInt("id"));
                int subId = rs.getInt("student_submission_id");
                r.setStudentSubmissionId(rs.wasNull() ? null : subId);
                r.setStudentId(rs.getString("student_id"));
                r.setStatus(ComparisonStatus.valueOf(rs.getString("status")));
                r.setActualOutput(rs.getString("actual_output"));
                r.setExpectedOutput(rs.getString("expected_output"));
                r.setDiffLines(JsonArrayCodec.decode(rs.getString("diff_lines")));
                r.setErrorMessage(rs.getString("error_message"));
                String nm = rs.getString("normalization_mode");
                r.setNormalizationMode(nm != null ? NormalizationMode.valueOf(nm) : null);
                r.setTimestamp(rs.getString("timestamp"));
                out.add(r);
            }
        }
        return out;
    }

    public void deleteAll() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM EVALUATION_RESULT");
        }
    }
}
