package org.jibe77.hermanas.service.logs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata about a log file available on disk")
public class LogFileInfo {

    @Schema(description = "File name (no path component)", example = "spring.log")
    private final String name;

    @Schema(description = "File size in bytes", example = "12345")
    private final long size;

    @Schema(description = "Last modification time as epoch millis", example = "1716625200000")
    private final long lastModified;

    public LogFileInfo(String name, long size, long lastModified) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
    }

    public String getName() { return name; }
    public long getSize() { return size; }
    public long getLastModified() { return lastModified; }
}
