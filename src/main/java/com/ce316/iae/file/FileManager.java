package com.ce316.iae.file;

import com.ce316.iae.model.Submission;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        List<Submission> submissions = new ArrayList<>();
        Path dirPath = Paths.get(zipDirectory);

        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            System.err.println("Invalid ZIP directory: " + zipDirectory);
            return submissions;
        }

        try {
            Files.list(dirPath)
                .filter(path -> path.toString().toLowerCase().endsWith(".zip"))
                .forEach(zipFile -> {
                    String studentId = extractStudentId(zipFile.getFileName().toString());
                    // Extract into a subfolder named "extracted/<studentId>" to keep things clean
                    String targetFolder = Paths.get(zipDirectory, "extracted", studentId).toString();

                    boolean extractionSuccess = extractZip(zipFile.toString(), targetFolder);
                    if (!extractionSuccess) {
                        System.err.println("Failed to extract: " + zipFile.toString());
                        return; // Move to the next student
                    }

                    String mainFile = resolveSourceFile(targetFolder, sourceFileName);
                    if (mainFile == null) {
                        System.err.println("Source file not found for: " + zipFile.toString());
                        return; // Move to the next student
                    }

                    submissions.add(new Submission(studentId, zipFile.toString(), targetFolder, mainFile));
                });
        } catch (IOException e) {
            System.err.println("Error reading ZIP directory: " + e.getMessage());
        }

        return submissions;
    }

    /**
     * Extracts a single ZIP file to the target directory.
     *
     * @param zipPath   The path to the ZIP file.
     * @param targetDir The directory to extract contents into.
     * @return true if extraction was successful, false otherwise.
     */
    private boolean extractZip(String zipPath, String targetDir) {
        File destDir = new File(targetDir);
        if (!destDir.exists() && !destDir.mkdirs()) {
            return false;
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(destDir, zipEntry.getName());
                
                // Protect against Zip Slip
                String destDirPath = destDir.getCanonicalPath();
                String destFilePath = newFile.getCanonicalPath();
                if (!destFilePath.startsWith(destDirPath + File.separator)) {
                    throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
                }

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to extract " + zipPath + ": " + e.getMessage());
            return false;
        }
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
