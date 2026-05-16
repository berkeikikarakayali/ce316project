package com.ce316.iae.service;

import com.ce316.iae.model.LanguageConfig;
import com.ce316.iae.model.Submission;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Batch ZIP ingestion using {@link ZipInputStream} (no shell unzip).
 */
public final class FileManager {

    public List<Submission> prepareSubmissions(Path zipDirectory,
                                               LanguageConfig languageConfig,
                                               String mainSourceFilenameOrNull) throws IOException {
        if (zipDirectory == null || !Files.isDirectory(zipDirectory)) {
            throw new IOException("ZIP directory does not exist: " + zipDirectory);
        }

        List<Path> zipFiles;
        try (Stream<Path> stream = Files.list(zipDirectory)) {
            zipFiles = stream
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }

        List<Submission> submissions = new ArrayList<>();
        for (Path zipPath : zipFiles) {
            String fileName = zipPath.getFileName().toString();
            String studentId = fileName.substring(0, fileName.length() - 4);

            Path targetDir = zipDirectory.resolve(studentId).normalize();
            if (!targetDir.startsWith(zipDirectory.normalize())) {
                continue;
            }

            Files.createDirectories(targetDir);

            if (!extractZip(zipPath, targetDir)) {
                System.err.println("[FileManager] corrupt ZIP skipped: " + zipPath);
                continue;
            }

            Path mainFile = resolveMainSource(targetDir, languageConfig, mainSourceFilenameOrNull);
            if (mainFile == null || !Files.isRegularFile(mainFile)) {
                System.err.println("[FileManager] source file not found for student " + studentId);
                continue;
            }

            submissions.add(new Submission(
                    studentId,
                    zipPath.toAbsolutePath().toString(),
                    targetDir.toAbsolutePath().toString(),
                    mainFile.toAbsolutePath().toString()));
        }

        return submissions;
    }

    private boolean extractZip(Path zipFile, Path targetDir) {
        try (InputStream fis = Files.newInputStream(zipFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName()).normalize();
                if (!outPath.startsWith(targetDir)) {
                    zis.closeEntry();
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    try (var out = Files.newOutputStream(outPath)) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                        }
                    }
                }
                zis.closeEntry();
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private Path resolveMainSource(Path extractedRoot,
                                   LanguageConfig languageConfig,
                                   String mainSourceFilenameOrNull) throws IOException {
        String ext = languageConfig.getFileExtension().trim()
                .replaceFirst("^\\.+", "")
                .toLowerCase(Locale.ROOT);
        String suffix = "." + ext;

        if (mainSourceFilenameOrNull != null && !mainSourceFilenameOrNull.isBlank()) {
            String wanted = mainSourceFilenameOrNull.trim();
            try (Stream<Path> walk = Files.walk(extractedRoot)) {
                List<Path> candidates = walk
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().equalsIgnoreCase(wanted))
                        .sorted()
                        .collect(Collectors.toList());
                return candidates.isEmpty() ? null : candidates.get(0);
            }
        }

        Path preferred = extractedRoot.resolve("main" + suffix);
        if (Files.isRegularFile(preferred)) {
            return preferred;
        }

        try (Stream<Path> walk = Files.walk(extractedRoot)) {
            List<Path> matches = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(suffix))
                    .sorted()
                    .collect(Collectors.toList());
            return matches.isEmpty() ? null : matches.get(0);
        }
    }
}
