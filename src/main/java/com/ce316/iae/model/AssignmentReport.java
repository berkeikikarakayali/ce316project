package com.ce316.iae.model;

public class AssignmentReport {

    private final int totalStudents;
    private final int passCount;
    private final int failCount;
    private final int errorCount;

    public AssignmentReport(int totalStudents, int passCount, int failCount, int errorCount) {
        this.totalStudents = totalStudents;
        this.passCount     = passCount;
        this.failCount     = failCount;
        this.errorCount    = errorCount; //including timeout, compile eror and eror
    }

    public int getTotalStudents() { return totalStudents; }
    public int getPassCount()     { return passCount; }
    public int getFailCount()     { return failCount; }
    public int getErrorCount()    { return errorCount; }

    public double getPassRate() {
        if (totalStudents == 0) return 0.0;
        return (double) passCount / totalStudents;
    }
}
