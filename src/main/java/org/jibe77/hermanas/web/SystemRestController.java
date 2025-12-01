package org.jibe77.hermanas.service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.controller.system.SystemService;
import org.jibe77.hermanas.security.audit.AuditLog;
import org.jibe77.hermanas.security.ratelimit.RateLimited;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
