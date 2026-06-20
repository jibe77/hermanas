package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.service.logs.LogFileInfo;
import org.jibe77.hermanas.service.logs.LogsService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "Logs", description = "Read application log files (admin only)")
public class LogsRestController {

    private final LogsService logsService;

    public LogsRestController(LogsService logsService) {
        this.logsService = logsService;
    }

    @Operation(summary = "List available log files",
            description = "Returns the log files present in the configured logs directory, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of files",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LogFileInfo.class))),
            @ApiResponse(responseCode = "500", description = "I/O error reading the directory")
    })
    @GetMapping
    public List<LogFileInfo> list() throws IOException {
        return logsService.listFiles();
    }

    @Operation(summary = "Read the tail of a log file",
            description = "Returns the last N lines of a log file, optionally filtered by level "
                    + "(TRACE/DEBUG/INFO/WARN/ERROR — keeps lines at the requested level or above) "
                    + "and/or by a free-text substring (case-insensitive). Supports both plain "
                    + ".log files and gzipped rotated files (.log.YYYY-MM-DD.N.gz).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log lines"),
            @ApiResponse(responseCode = "400", description = "Invalid filename or path traversal attempt"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{filename:.+}")
    public ResponseEntity<List<String>> tail(
            @Parameter(description = "Log file name (must exist in the configured directory)", example = "spring.log")
            @PathVariable String filename,
            @Parameter(description = "Number of lines to return (default 500, max 5000)", example = "500")
            @RequestParam(defaultValue = "500") @Min(1) @Max(5000) int lines,
            @Parameter(description = "Minimum log level to keep (TRACE/DEBUG/INFO/WARN/ERROR, or ALL/empty for no filter)", example = "WARN")
            @RequestParam(required = false) String level,
            @Parameter(description = "Case-insensitive substring filter", example = "DoorService")
            @RequestParam(required = false) String search) {
        try {
            return ResponseEntity.ok(logsService.tail(filename, lines, level, search));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(List.of("Bad request: " + e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(List.of("Cannot read log file: " + e.getMessage()));
        }
    }

    @Operation(summary = "Download a log file",
            description = "Streams the full log file as an attachment. Gzipped rotated files are "
                    + "decompressed on the fly so the downloaded artefact is always plain text.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File stream"),
            @ApiResponse(responseCode = "400", description = "Invalid filename or path traversal attempt"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    @GetMapping("/{filename:.+}/download")
    public ResponseEntity<InputStreamResource> download(
            @Parameter(description = "Log file name (must exist in the configured directory)", example = "spring.log")
            @PathVariable String filename) {
        try {
            LogsService.LogStream stream = logsService.openForDownload(filename);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(
                    org.springframework.http.ContentDisposition
                            .attachment()
                            .filename(stream.getDownloadName())
                            .build());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(stream.getInputStream()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
