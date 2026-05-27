package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.service.system.SystemService;
import org.jibe77.hermanas.security.audit.AuditLog;
import org.jibe77.hermanas.security.ratelimit.RateLimited;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System control endpoints for shutdown and reboot operations")
public class SystemRestController {

    SystemService systemService;

    public SystemRestController(SystemService systemService) {
        this.systemService = systemService;
    }

    @Operation(
            summary = "Shutdown system",
            description = "Initiates a system shutdown (Raspberry Pi will power off)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Shutdown initiated successfully"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded - too many shutdown attempts"
            )
    })
    @AuditLog(category = "SYSTEM", operation = "System shutdown initiated")
    @RateLimited(maxRequests = 2, windowSeconds = 300, message = "Too many shutdown attempts. Please wait 5 minutes.")
    @PostMapping(value = "/shutdown")
    public void shutdown() {
        systemService.shutdown();
    }

    @Operation(
            summary = "Reboot system",
            description = "Initiates a system reboot (Raspberry Pi will restart)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Reboot initiated successfully"
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Rate limit exceeded - too many reboot attempts"
            )
    })
    @AuditLog(category = "SYSTEM", operation = "System reboot initiated")
    @RateLimited(maxRequests = 2, windowSeconds = 300, message = "Too many reboot attempts. Please wait 5 minutes.")
    @PostMapping(value = "/reboot")
    public void reboot() {
        systemService.reboot();
    }

    @Operation(
            summary = "Disk usage of the partition hosting the application",
            description = "Returns total, used and free bytes plus the usage percentage of the filesystem " +
                    "partition that contains the application working directory. Intended for the diagnostics " +
                    "panel — admin only."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Disk usage retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Authenticated user is not an administrator")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/disk-usage")
    public Map<String, Object> diskUsage() {
        File root = new File(".").getAbsoluteFile();
        long total = root.getTotalSpace();
        long free = root.getFreeSpace();
        long used = total - free;
        double usedPercent = total > 0 ? (used * 100.0) / total : 0.0;

        Map<String, Object> response = new HashMap<>();
        response.put("path", root.getAbsolutePath());
        response.put("totalBytes", total);
        response.put("usedBytes", used);
        response.put("freeBytes", free);
        response.put("usedPercent", Math.round(usedPercent * 10.0) / 10.0);
        return response;
    }
}
