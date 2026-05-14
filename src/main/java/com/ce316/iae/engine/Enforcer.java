package com.ce316.iae.engine;

import java.io.*;
import java.util.List;
import java.util.concurrent.*;

/*
 handbook for how to use:
 Enforcer e = new Enforcer();
 e.execute(List.of("gcc", "-o", "main", "main.c"), "/student/dir", 60);
 System.out.println(e.getOutput());
 */

public class Enforcer {
    // result alanları execute() çağrısından sonra dolu olur
    private String stdout   = "";
    private String stderr   = "";
    private int    exitCode = -1;
    private boolean timedOut = false;


    private static final int MAX_OUTPUT_BYTES = 1024 * 1024;


    public void execute(List<String> command, String workingDirectory, int timeoutSeconds) {
        // reset
        stdout   = "";
        stderr   = "";
        exitCode = -1;
        timedOut = false;

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workingDirectory));
        pb.redirectErrorStream(false); // stdout ve stderr ayrı yakalanıyo

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            stderr   = "Process couldn't be started: " + e.getMessage();
            exitCode = -1;
            return;
        }

        // buffer dolmasın diye stdout ve stderr ayrı threadlerde;
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Future<String> stdoutFuture = pool.submit(() -> readStream(process.getInputStream()));
        Future<String> stderrFuture = pool.submit(() -> readStream(process.getErrorStream()));

        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            stderr   = "Process cut while waiting.";
            timedOut = true;
            pool.shutdownNow();
            return;
        }

        if (!finished) {
            // timeout
            process.destroyForcibly();
            timedOut = true;
            exitCode = -1;
        } else {
            exitCode = process.exitValue();
        }

        try {
            stdout = stdoutFuture.get(5, TimeUnit.SECONDS);
            stderr = stderrFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (stdout == null) stdout = "";
            if (stderr == null) stderr = "";
        }
        pool.shutdownNow();
    }

    private String readStream(InputStream is) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            int total = 0;
            while ((read = reader.read(buf)) != -1) {
                total += read;
                if (total > MAX_OUTPUT_BYTES) {
                    sb.append("\n[1 MB limit exeeded!!!]");
                    break;
                }
                sb.append(buf, 0, read);
            }
            return sb.toString();
        } catch (IOException e) {
            return "[Stream reading error: " + e.getMessage() + "]";
        }
    }

    // getters

    // process default çıktı (stdout)
    public String getOutput()   { return stdout; }

    // process error çıktısı (stderr)
    public String getError()    { return stderr; }

    //process exit çıktısı
    public int    getExitCode() { return exitCode; }

    //timeout kill
    public boolean didTimeout() { return timedOut; }

}
