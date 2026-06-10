package org.jibe77.hermanas.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.data.entity.HermanasUser;
import org.jibe77.hermanas.data.repository.HermanasUserRepository;
import org.jibe77.hermanas.service.camera.CameraService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/email")
@Tag(name = "Email", description = "Email notification diagnostics")
public class EmailRestController {

    private static final Logger logger = LoggerFactory.getLogger(EmailRestController.class);

    private final EmailService emailService;
    private final CameraService cameraService;
    private final HermanasUserRepository userRepository;

    public EmailRestController(EmailService emailService, CameraService cameraService,
                               HermanasUserRepository userRepository) {
        this.emailService = emailService;
        this.cameraService = cameraService;
        this.userRepository = userRepository;
    }

    @Operation(
            summary = "Send a test email",
            description = "Sends a synchronous test email to the email address of the authenticated " +
                    "user. Used to verify SMTP configuration from the diagnostics panel."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test email sent successfully"),
            @ApiResponse(responseCode = "409", description = "Email notifications are disabled or user has no email"),
            @ApiResponse(responseCode = "502", description = "SMTP send failed")
    })
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestEmail(Authentication authentication) {
        String recipient = resolveAuthenticatedUserEmail(authentication);
        if (recipient == null || recipient.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Collections.singletonMap(
                    "message",
                    "No email address on your user account. Set one in your profile before testing."));
        }
        try {
            Optional<File> picture = cameraService.takePictureNoException(true);
            emailService.sendTestMail(picture, recipient);
            String detail = picture.isPresent()
                    ? "Test email sent with picture to " + recipient + "."
                    : "Test email sent to " + recipient + " (no picture available).";
            return ResponseEntity.ok(Collections.singletonMap("message", detail));
        } catch (IllegalStateException ex) {
            logger.warn("Test email rejected: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Collections.singletonMap("message", ex.getMessage()));
        } catch (MailException ex) {
            logger.error("Test email failed to send.", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Collections.singletonMap("message", "SMTP send failed: " + ex.getMessage()));
        }
    }

    private String resolveAuthenticatedUserEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userRepository.findByLogin(authentication.getName())
                .map(HermanasUser::getEmail)
                .orElse(null);
    }
}
