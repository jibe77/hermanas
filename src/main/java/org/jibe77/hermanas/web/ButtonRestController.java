package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.service.birdhouse.BirdhouseButtonService;
import org.jibe77.hermanas.service.door.bottombutton.BottomButtonService;
import org.jibe77.hermanas.service.door.upbutton.UpButtonService;
import org.jibe77.hermanas.websocket.Button;
import org.jibe77.hermanas.websocket.ButtonStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.annotation.Lazy;

import java.util.Arrays;
import java.util.List;

@Lazy
@RestController
@RequestMapping("/api/v1/buttons")
@Tag(name = "Buttons", description = "End-stop button (limit switch) status endpoints for diagnostics")
public class ButtonRestController {

    private final UpButtonService upButtonService;
    private final BottomButtonService bottomButtonService;
    private final BirdhouseButtonService birdhouseButtonService;

    public ButtonRestController(UpButtonService upButtonService,
                                BottomButtonService bottomButtonService,
                                BirdhouseButtonService birdhouseButtonService) {
        this.upButtonService = upButtonService;
        this.bottomButtonService = bottomButtonService;
        this.birdhouseButtonService = birdhouseButtonService;
    }

    @Operation(
            summary = "Get current status of all end-stop buttons",
            description = "Returns the live pressed/released state of upper and bottom limit switches. " +
                    "Useful for diagnostics when a switch is suspected to be broken (always pressed or never triggered)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Button statuses retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = ButtonStatus.class)))
            )
    })
    @GetMapping("/status")
    public List<ButtonStatus> status() {
        long now = System.currentTimeMillis();
        return Arrays.asList(
                new ButtonStatus(Button.UP, upButtonService.isUpButtonPressed(), now),
                new ButtonStatus(Button.BOTTOM, bottomButtonService.isBottomButtonPressed(), now),
                new ButtonStatus(Button.BIRDHOUSE, birdhouseButtonService.isButtonPressed(), now)
        );
    }
}
