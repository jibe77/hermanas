package org.jibe77.hermanas.service.resident;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Stores resident photos on the local filesystem outside the JAR.
 * Path configurable via {@code hermanas.residents.photos-dir}.
 */
@Service
public class ResidentPhotoStorage {

    private static final Logger logger = LoggerFactory.getLogger(ResidentPhotoStorage.class);

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp"
    );

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final String photosDir;

    private Path photosPath;

    public ResidentPhotoStorage(@Value("${hermanas.residents.photos-dir:./residents-photos}") String photosDir) {
        this.photosDir = photosDir;
    }

    @PostConstruct
    void init() throws IOException {
        photosPath = Paths.get(photosDir).toAbsolutePath().normalize();
        Files.createDirectories(photosPath);
        logger.info("Resident photos directory: {}", photosPath);
    }

    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Photo exceeds 5 MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported image type: " + contentType);
        }
        String extension = extensionFor(contentType);
        String filename = UUID.randomUUID() + extension;
        Path target = photosPath.resolve(filename);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return filename;
    }

    public Path resolve(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        Path resolved = photosPath.resolve(filename).normalize();
        if (!resolved.startsWith(photosPath)) {
            throw new IllegalArgumentException("Path traversal attempt blocked");
        }
        return resolved;
    }

    public void delete(String filename) {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        try {
            Path path = resolve(filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete photo {}: {}", filename, e.getMessage());
        }
    }

    public String contentTypeFor(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }

    private String extensionFor(String contentType) {
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/webp":
                return ".webp";
            default:
                return "";
        }
    }
}
