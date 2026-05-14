package com.ce316.iae.file;

import com.ce316.iae.model.Submission;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

/**
 * Handles extracting student submissions and locating their source files.
 */
public class FileManager {

    /**
     * Scans the directory for ZIP files, extracts them, and finds the required source file.
     *
     * @param zipDirectory   The directory containing student ZIP files.
     * @param sourceFileName The name of the main source file to look for (e.g., "main.c").
     * @return A list of valid Submission objects.
     */
    public List<Submission> prepareSubmissions(String zipDirectory, String sourceFileName) {
        // TODO: Implement main processing loop
        return new ArrayList<>();
    }

    /**
     * Extracts a single ZIP file to the target directory.
     *
     * @param zipPath   The path to the ZIP file.
     * @param targetDir The directory to extract contents into.
     * @return true if extraction was successful, false otherwise.
     */
    private boolean extractZip(String zipPath, String targetDir) {
        // TODO: Implement using java.util.zip
        return false;
    }

    /**
     * Recursively searches the extracted folder for the required source file.
     *
     * @param folder   The directory to search in.
     * @param fileName The name of the file to find.
     * @return The absolute path to the found file, or null if not found.
     */
    private String resolveSourceFile(String folder, String fileName) {
        try {
            Path startPath = Paths.get(folder);
            if (!Files.exists(startPath)) {
                return null;
            }

            return Files.walk(startPath)
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().equals(fileName))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            System.err.println("Error searching for source file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Derives the student ID by stripping the .zip extension from the filename.
     *
     * @param zipFile The name of the ZIP file.
     * @return The student ID.
     */
    private String extractStudentId(String zipFile) {
        if (zipFile == null) return null;
        String lowerCaseName = zipFile.toLowerCase();
        if (lowerCaseName.endsWith(".zip")) {
            return zipFile.substring(0, zipFile.length() - 4);
        }
        return zipFile;
    }
}
