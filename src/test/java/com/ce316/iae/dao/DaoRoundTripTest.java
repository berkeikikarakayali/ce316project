package com.ce316.iae.dao;

import com.ce316.iae.db.DatabaseService;
import com.ce316.iae.model.ComparisonStatus;
import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.NormalizationMode;
import com.ce316.iae.model.Project;
import com.ce316.iae.model.StudentReport;
import com.ce316.iae.model.Submission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DaoRoundTripTest {

    private DatabaseService service;
    private Path iae;

    @BeforeEach
    void setUp(@TempDir Path tmp) throws Exception {
        iae = tmp.resolve("daotest.iae");
        service = new DatabaseService();
        service.createNewProject(iae);
    }

    @AfterEach
    void tearDown() {
        if (service != null) service.close();
    }

    @Test
    void configurationDao_insertFindUpdateDelete() throws Exception {
        ConfigurationDAO dao = new ConfigurationDAO(service.connection());
        LanguageConfig java = new LanguageConfig(
                "Java", "java", "/usr/bin/javac",
                Arrays.asList("-d", "."),
                Arrays.asList("java", "Main"));

        int id = dao.insert(java);
        assertTrue(id > 0);

        LanguageConfig fetched = dao.findByLanguage("Java");
        assertNotNull(fetched);
        assertEquals("java", fetched.getFileExtension());
        assertEquals(Arrays.asList("-d", "."), fetched.getCompileArgs());
        assertEquals(Arrays.asList("java", "Main"), fetched.getRunArgs());

        fetched.setCompilerPath("/opt/jdk/bin/javac");
        dao.update(fetched);
        assertEquals("/opt/jdk/bin/javac", dao.findByLanguage("Java").getCompilerPath());

        assertEquals(1, dao.findAll().size());
        dao.delete("Java");
        assertNull(dao.findByLanguage("Java"));
        assertEquals(0, dao.findAll().size());
    }

    @Test
    void projectDao_loadReturnsBlankRowAfterCreate() throws Exception {
        ProjectDAO dao = new ProjectDAO(service.connection());
        Project p = dao.loadProject();
        assertNotNull(p);
        assertEquals(60, p.getCompileTimeoutSec());
        assertEquals(30, p.getRunTimeoutSec());
        assertEquals(NormalizationMode.STRICT, p.getNormalizationMode());
    }

    @Test
    void projectDao_updateAndReload() throws Exception {
        ConfigurationDAO cfgDao = new ConfigurationDAO(service.connection());
        int cfgId = cfgDao.insert(new LanguageConfig(
                "C", "c", "/usr/bin/gcc",
                Arrays.asList("-O2"), Arrays.asList("./main")));

        ProjectDAO dao = new ProjectDAO(service.connection());
        Project p = dao.loadProject();
        p.setName("HW1 Sorting");
        p.setConfigurationId(cfgId);
        p.setExpectedOutputPath("C:/expected.txt");
        p.setZipFolderPath("D:/subs/zips");
        p.setMainSourceFilename("main.c");
        p.setRunArgs("--input case1");
        p.setCompileTimeoutSec(120);
        p.setRunTimeoutSec(45);
        p.setNormalizationMode(NormalizationMode.TRIM_WHITESPACE);
        dao.updateProject(p);

        Project reloaded = dao.loadProject();
        assertEquals("HW1 Sorting", reloaded.getName());
        assertEquals(cfgId, reloaded.getConfigurationId());
        assertEquals("C:/expected.txt", reloaded.getExpectedOutputPath());
        assertEquals("D:/subs/zips", reloaded.getZipFolderPath());
        assertEquals("main.c", reloaded.getMainSourceFilename());
        assertEquals("--input case1", reloaded.getRunArgs());
        assertEquals(120, reloaded.getCompileTimeoutSec());
        assertEquals(45, reloaded.getRunTimeoutSec());
        assertEquals(NormalizationMode.TRIM_WHITESPACE, reloaded.getNormalizationMode());
    }

    @Test
    void submissionAndResultRoundTrip_survivesCloseAndReopen() throws Exception {
        StudentSubmissionDAO subDao = new StudentSubmissionDAO(service.connection());
        Submission s1 = new Submission("20210001", "C:/zips/20210001.zip",
                "C:/extracted/20210001", "C:/extracted/20210001/main.c");
        Submission s2 = new Submission("20210002", "C:/zips/20210002.zip",
                "C:/extracted/20210002", "C:/extracted/20210002/main.c");
        subDao.insertAll(Arrays.asList(s1, s2));
        assertNotNull(s1.getId());
        assertNotNull(s2.getId());

        EvaluationResultDAO resDao = new EvaluationResultDAO(service.connection());
        StudentReport r1 = new StudentReport();
        r1.setStudentSubmissionId(s1.getId());
        r1.setStatus(ComparisonStatus.PASS);
        r1.setActualOutput("42\n");
        r1.setExpectedOutput("42\n");
        r1.setNormalizationMode(NormalizationMode.STRICT);
        r1.setTimestamp("2026-05-11T10:00:00");

        StudentReport r2 = new StudentReport();
        r2.setStudentSubmissionId(s2.getId());
        r2.setStatus(ComparisonStatus.FAIL);
        r2.setActualOutput("41\n");
        r2.setExpectedOutput("42\n");
        r2.setDiffLines(Arrays.asList("expected 42, got 41"));
        r2.setNormalizationMode(NormalizationMode.STRICT);
        r2.setTimestamp("2026-05-11T10:00:05");

        resDao.insertAll(Arrays.asList(r1, r2));

        service.closeProject();
        service.openProject(iae);

        List<Submission> reSubs = new StudentSubmissionDAO(service.connection()).findAll();
        assertEquals(2, reSubs.size());
        assertEquals("20210001", reSubs.get(0).getStudentId());

        List<StudentReport> reResults = new EvaluationResultDAO(service.connection()).findAll();
        assertEquals(2, reResults.size());
        assertEquals(ComparisonStatus.PASS, reResults.get(0).getStatus());
        assertEquals(ComparisonStatus.FAIL, reResults.get(1).getStatus());
        assertEquals(Arrays.asList("expected 42, got 41"), reResults.get(1).getDiffLines());
    }

    @Test
    void deleteAll_clearsTables() throws Exception {
        StudentSubmissionDAO subDao = new StudentSubmissionDAO(service.connection());
        subDao.insertAll(Arrays.asList(
                new Submission("S1", "a.zip", "a", "a/main.c")));
        assertEquals(1, subDao.findAll().size());
        subDao.deleteAll();
        assertEquals(0, subDao.findAll().size());
    }
}
