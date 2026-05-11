package com.ce316.iae.model;

public class Submission {
    private Integer id;
    private String studentId;
    private String zipFilePath;
    private String extractedFolderPath;
    private String mainSourceFile;

    public Submission() {}

    public Submission(String studentId, String zipFilePath,
                      String extractedFolderPath, String mainSourceFile) {
        this.studentId = studentId;
        this.zipFilePath = zipFilePath;
        this.extractedFolderPath = extractedFolderPath;
        this.mainSourceFile = mainSourceFile;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getZipFilePath() { return zipFilePath; }
    public void setZipFilePath(String zipFilePath) { this.zipFilePath = zipFilePath; }

    public String getExtractedFolderPath() { return extractedFolderPath; }
    public void setExtractedFolderPath(String extractedFolderPath) { this.extractedFolderPath = extractedFolderPath; }

    public String getMainSourceFile() { return mainSourceFile; }
    public void setMainSourceFile(String mainSourceFile) { this.mainSourceFile = mainSourceFile; }
}
