package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/info")
@Tag(name = "Info", description = "Application information endpoints for version and build details")
public class InfoRestController {

    BuildProperties buildProperties;

    public InfoRestController() {
       // default constructor, when build properties are not available.
    }

    @Autowired(required = false)
    public InfoRestController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Operation(
            summary = "Get application version",
            description = "Returns build information including version, artifact, group, and build time"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Build information retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BuildProperties.class))
            )
    })
    @GetMapping
    public BuildProperties version() {
        return buildProperties;
    }

}
