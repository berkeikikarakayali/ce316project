package com.ce316.iae.engine;

/*
öğrencilerin compile+run döngüsünün sonucu tutuyor
execution engine nesneyi yaratıyor "ReportingService" kaydediyor
*/

public class Executed {

    public boolean compSuccess;
    public boolean runSuccess;
    public String output;
    public String errMessage;
    public boolean timedOut;

    public Executed() {
        this.compSuccess = false;
        this.runSuccess  = false;
        this.output      = "";
        this.errMessage  = "";
        this.timedOut    = false;
    }

    @Override
    public String toString() {
        return "Executed{"
                + "compSuccess=" + compSuccess
                + ", runSuccess=" + runSuccess
                + ", timedOut="   + timedOut
                + ", output='"    + output.trim() + "'"
                + ", errMessage='" + errMessage.trim() + "'"
                + "}";
    }
}
