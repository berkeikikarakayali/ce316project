package com.ce316.iae.model;

public final class AssignmentReport {
    private final int totalStudents;
    private final int passCount;
    private final int failCount;
    private final int errorCount;

    public AssignmentReport(int totalStudents, int passCount, int failCount, int errorCount) {
        this.totalStudents = totalStudents;
        this.passCount = passCount;
        this.failCount = failCount;
        this.errorCount = errorCount;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public int getPassCount() {
        return passCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    /** Between {@code 0.0} and {@code 1.0}; zero total yields {@code 0.0}. */
    public double getPassRate() {
        return totalStudents <= 0 ? 0.0 : ((double) passCount / (double) totalStudents);
    }
}
