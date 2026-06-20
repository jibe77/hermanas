package org.jibe77.hermanas.service.logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Reads log files from the configured directory. The directory is locked down to a single
 * normalized base path, so callers can only address files inside it (no path traversal).
 */
@Service
public class LogsService {

    private static final Logger logger = LoggerFactory.getLogger(LogsService.class);

    private static final int MAX_TAIL_LINES = 5000;
    private static final int DEFAULT_TAIL_LINES = 500;
    private static final long READ_BUFFER_SIZE = 8 * 1024L;
    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "\\b(TRACE|DEBUG|INFO|WARN|ERROR)\\b");

    private final Path baseDirectory;

    public LogsService(@Value("${hermanas.logs.directory:./log}") String directory) {
        this.baseDirectory = Paths.get(directory).toAbsolutePath().normalize();
        logger.info("Logs base directory resolved to {}", baseDirectory);
    }

    public List<LogFileInfo> listFiles() throws IOException {
        if (!Files.isDirectory(baseDirectory)) {
            return new ArrayList<>();
        }
        List<LogFileInfo> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDirectory, this::isLogFile)) {
            for (Path p : stream) {
                result.add(new LogFileInfo(
                        p.getFileName().toString(),
                        Files.size(p),
                        Files.getLastModifiedTime(p).toMillis()));
            }
        }
        result.sort(Comparator.comparingLong(LogFileInfo::getLastModified).reversed());
        return result;
    }

    public List<String> tail(String filename, int requestedLines, String level, String search) throws IOException {
        Path file = resolveSafe(filename);
        int lines = clampLines(requestedLines);
        List<String> raw = isGzipped(file) ? readLastLinesGzip(file, lines) : readLastLines(file, lines);
        return filter(raw, level, search);
    }

    /**
     * Opens a streaming handle to the full file for the download endpoint. Gzipped
     * rotated files are decompressed on the fly and exposed under their plain-text
     * name (stripping the trailing {@code .gz}) so the user gets a readable artefact.
     */
    public LogStream openForDownload(String filename) throws IOException {
        Path file = resolveSafe(filename);
        InputStream raw = Files.newInputStream(file);
        String downloadName = file.getFileName().toString();
        if (isGzipped(file)) {
            // Wrap in a GZIPInputStream so the client receives plain text. The
            // returned stream owns the underlying file handle and closes it via
            // the standard try-with / Spring close-on-complete contract.
            InputStream gz = new GZIPInputStream(raw);
            if (downloadName.toLowerCase().endsWith(".gz")) {
                downloadName = downloadName.substring(0, downloadName.length() - 3);
            }
            return new LogStream(gz, downloadName);
        }
        return new LogStream(raw, downloadName);
    }

    /** Stream + suggested filename pair used by the download endpoint. */
    public static final class LogStream {
        private final InputStream inputStream;
        private final String downloadName;

        LogStream(InputStream inputStream, String downloadName) {
            this.inputStream = inputStream;
            this.downloadName = downloadName;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getDownloadName() {
            return downloadName;
        }
    }

    private boolean isGzipped(Path file) {
        return file.getFileName().toString().toLowerCase().endsWith(".gz");
    }

    /**
     * Reads a gzipped log line by line through a bounded FIFO that keeps only the last {@code count}
     * lines. Memory stays O(count) but we still pay full decompression cost — acceptable since the
     * rotated files in this project are ~30-250 KB compressed.
     */
    private List<String> readLastLinesGzip(Path file, int count) throws IOException {
        Deque<String> tail = new ArrayDeque<>(count);
        try (InputStream fis = Files.newInputStream(file);
             GZIPInputStream gis = new GZIPInputStream(fis);
             BufferedReader reader = new BufferedReader(new InputStreamReader(gis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (tail.size() == count) {
                    tail.pollFirst();
                }
                tail.addLast(line);
            }
        }
        return new ArrayList<>(tail);
    }

    private List<String> filter(List<String> lines, String level, String search) {
        String normalizedLevel = level == null ? null : level.trim().toUpperCase();
        String normalizedSearch = search == null ? null : search.trim();
        if ((normalizedLevel == null || normalizedLevel.isEmpty())
                && (normalizedSearch == null || normalizedSearch.isEmpty())) {
            return lines;
        }
        return lines.stream()
                .filter(line -> matchesLevel(line, normalizedLevel))
                .filter(line -> normalizedSearch == null || normalizedSearch.isEmpty()
                        || line.toLowerCase().contains(normalizedSearch.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Keeps lines whose level is at or above the requested threshold. Lines without a recognized
     * level token (continuation lines of stack traces) are kept attached to the previous match
     * conceptually — but here we keep them only when no level filter is active, otherwise they
     * would clutter ERROR-only views. Tradeoff: stack traces only appear on level=ALL/empty.
     */
    private boolean matchesLevel(String line, String requestedLevel) {
        if (requestedLevel == null || requestedLevel.isEmpty() || "ALL".equals(requestedLevel)) {
            return true;
        }
        var matcher = LEVEL_PATTERN.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        int lineRank = rank(matcher.group(1));
        int requestedRank = rank(requestedLevel);
        return lineRank >= requestedRank;
    }

    private int rank(String level) {
        switch (level) {
            case "TRACE": return 0;
            case "DEBUG": return 1;
            case "INFO": return 2;
            case "WARN": return 3;
            case "ERROR": return 4;
            default: return -1;
        }
    }

    private Path resolveSafe(String filename) throws IOException {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("filename is required");
        }
        Path candidate = baseDirectory.resolve(filename).normalize();
        if (!candidate.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("path escapes the logs directory");
        }
        if (!Files.isRegularFile(candidate)) {
            throw new IOException("log file not found: " + filename);
        }
        if (!isLogFile(candidate)) {
            throw new IllegalArgumentException("not a log file: " + filename);
        }
        return candidate;
    }

    private boolean isLogFile(Path p) {
        String name = p.getFileName().toString().toLowerCase();
        return name.endsWith(".log") || name.matches(".*\\.log\\.\\d{4}-\\d{2}-\\d{2}(\\.\\d+)?(\\.gz)?")
                || name.matches(".*\\.log\\.\\d+(\\.gz)?");
    }

    private int clampLines(int requested) {
        if (requested <= 0) {
            return DEFAULT_TAIL_LINES;
        }
        return Math.min(requested, MAX_TAIL_LINES);
    }

    /**
     * Reads the last {@code count} lines from the file by reading the file in reverse, one buffer
     * at a time, splitting on newlines. Memory usage stays O(count), not O(file size).
     */
    private List<String> readLastLines(Path file, int count) throws IOException {
        List<String> result = new ArrayList<>(count);
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long length = raf.length();
            if (length == 0) {
                return result;
            }
            long position = length;
            byte[] buffer = new byte[(int) Math.min(READ_BUFFER_SIZE, length)];
            StringBuilder lineBuilder = new StringBuilder();
            int linesFound = 0;
            while (position > 0 && linesFound <= count) {
                int readSize = (int) Math.min(buffer.length, position);
                position -= readSize;
                raf.seek(position);
                raf.readFully(buffer, 0, readSize);
                for (int i = readSize - 1; i >= 0; i--) {
                    char c = (char) (buffer[i] & 0xff);
                    if (c == '\n') {
                        if (lineBuilder.length() > 0 || !result.isEmpty() || position + i < length - 1) {
                            result.add(0, lineBuilder.reverse().toString());
                            linesFound++;
                            lineBuilder.setLength(0);
                            if (linesFound >= count) {
                                return decode(result);
                            }
                        }
                    } else if (c != '\r') {
                        lineBuilder.append(c);
                    }
                }
            }
            if (lineBuilder.length() > 0) {
                result.add(0, lineBuilder.reverse().toString());
            }
        }
        return decode(result);
    }

    /**
     * The byte-by-byte reverse read above treats each byte as a char, which works for ASCII but
     * mangles multi-byte UTF-8 sequences. Re-encode each line by round-tripping through bytes so
     * accented chars (logs occasionally contain French strings) come out right.
     */
    private List<String> decode(List<String> lines) {
        List<String> decoded = new ArrayList<>(lines.size());
        for (String raw : lines) {
            byte[] bytes = new byte[raw.length()];
            for (int i = 0; i < raw.length(); i++) {
                bytes[i] = (byte) raw.charAt(i);
            }
            decoded.add(new String(bytes, StandardCharsets.UTF_8));
        }
        return decoded;
    }
}
