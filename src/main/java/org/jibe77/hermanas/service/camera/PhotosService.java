package org.jibe77.hermanas.service.camera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Read-only file explorer over the camera pictures directory. Only directories and image files
 * with allow-listed extensions are visible; any attempt to escape the base directory via
 * {@code ../} or absolute paths is rejected.
 */
@Service
public class PhotosService {

    private static final Logger logger = LoggerFactory.getLogger(PhotosService.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final Path baseDirectory;

    public PhotosService(@Value("${camera.path.root:./photos}") String directory) {
        this.baseDirectory = Paths.get(directory).toAbsolutePath().normalize();
        logger.info("Photos base directory resolved to {}", baseDirectory);
    }

    /**
     * Lists the immediate children of {@code relativePath} inside the photos directory.
     * Directories come first (newest name first — works because the on-disk layout is
     * {@code YYYY/MM/DD}), then the image files. Hidden entries (starting with {@code .})
     * are skipped.
     */
    public ListingResult list(String relativePath) throws IOException {
        Path dir = resolveDirectorySafe(relativePath);

        List<Entry> directories = new ArrayList<>();
        List<Entry> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                String name = p.getFileName().toString();
                if (name.startsWith(".")) {
                    continue;
                }
                if (Files.isDirectory(p)) {
                    directories.add(new Entry(name, "DIRECTORY", 0L,
                            Files.getLastModifiedTime(p).toMillis()));
                } else if (Files.isRegularFile(p) && isImage(name)) {
                    directories.size(); // no-op, keeps IntelliJ happy when files list is empty
                    files.add(new Entry(name, "FILE", Files.size(p),
                            Files.getLastModifiedTime(p).toMillis()));
                }
            }
        }
        // Both sorted by name descending so newest (year/month/day or YYYY-MM-DD-HHMMSS pictures)
        // appear first — matches user expectation when browsing the chicken-coop archive.
        Comparator<Entry> byNameDesc = Comparator.comparing(Entry::getName).reversed();
        directories.sort(byNameDesc);
        files.sort(byNameDesc);
        return new ListingResult(normaliseForClient(dir), directories, files);
    }

    /**
     * Resolves a relative image path to an absolute one, ready for streaming back to the client.
     * Throws {@link IllegalArgumentException} on traversal attempts or on non-image files.
     */
    public Path resolveImageFile(String relativePath) throws IOException {
        if (relativePath == null || relativePath.trim().isEmpty()) {
            throw new IllegalArgumentException("path is required");
        }
        Path candidate = baseDirectory.resolve(relativePath).normalize();
        if (!candidate.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("path escapes the photos directory");
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IOException("file not found: " + relativePath);
        }
        if (!isImage(candidate.getFileName().toString())) {
            throw new IllegalArgumentException("not an image file: " + relativePath);
        }
        return candidate;
    }

    /**
     * Same path-traversal guard as {@link #resolveImageFile} but for the listing endpoint —
     * an empty/null path means "the root", which {@link #resolveImageFile} would reject.
     */
    private Path resolveDirectorySafe(String relativePath) throws IOException {
        Path candidate;
        if (relativePath == null || relativePath.trim().isEmpty() || "/".equals(relativePath)) {
            candidate = baseDirectory;
        } else {
            candidate = baseDirectory.resolve(relativePath).normalize();
        }
        if (!candidate.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("path escapes the photos directory");
        }
        if (!Files.isDirectory(candidate)) {
            throw new IOException("directory not found: " + relativePath);
        }
        return candidate;
    }

    private boolean isImage(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return false;
        }
        String ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    /** Relative path of {@code dir} from {@link #baseDirectory}, in URL form (forward slashes). */
    private String normaliseForClient(Path dir) {
        Path rel = baseDirectory.relativize(dir);
        String s = rel.toString().replace('\\', '/');
        return s.isEmpty() ? "" : s;
    }

    public static final class Entry {
        private final String name;
        private final String type;       // "DIRECTORY" | "FILE"
        private final long size;
        private final long lastModified;

        public Entry(String name, String type, long size, long lastModified) {
            this.name = name;
            this.type = type;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getName() { return name; }
        public String getType() { return type; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
    }

    public static final class ListingResult {
        private final String path;
        private final List<Entry> directories;
        private final List<Entry> files;

        public ListingResult(String path, List<Entry> directories, List<Entry> files) {
            this.path = path;
            this.directories = directories;
            this.files = files;
        }

        public String getPath() { return path; }
        public List<Entry> getDirectories() { return directories; }
        public List<Entry> getFiles() { return files; }
    }
}
