package org.jibe77.hermanas.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * Replaces Spring Boot's Whitelabel error page with a redirect to the Angular
 * SPA shell. The SPA owns its own 404 component (the animated chicken) and we
 * want every browser-facing miss to land on it, regardless of whether the
 * visitor typed {@code /foobar}, {@code /fr-FR/foobar} or hit a real 500.
 *
 * <p>API errors are untouched: {@code /api/v1/**} responses are produced by
 * the REST controllers and {@link org.jibe77.hermanas.exception.GlobalExceptionHandler},
 * which already return JSON with the right status code. We only intercept the
 * generic {@code /error} forward that Spring uses for non-API browser hits.</p>
 */
@Controller
public class SpaErrorController implements ErrorController {

    /** Default locale of the bundled SPA — same one used by {@link WebConfig}. */
    private static final String DEFAULT_LOCALE_PREFIX = "/fr-FR";

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        // Surface API-originated errors as their plain status — the SPA shell
        // would only confuse a non-browser client. WebConfig forwards already
        // shield /api/**, /actuator/**, /v3/**, /swagger-ui/** from the SPA
        // catch-all, so any error we see here on those paths is a genuine
        // problem worth showing as-is.
        if (originalUri instanceof String) {
            String uri = (String) originalUri;
            if (uri.startsWith("/api/")
                    || uri.startsWith("/actuator/")
                    || uri.startsWith("/v3/")
                    || uri.startsWith("/swagger-ui/")
                    || uri.startsWith("/stomp")) {
                return null; // let Spring's default behaviour finish the response
            }
        }

        // 404 / no-mapping / typo'd path — forward to the SPA shell so Angular's
        // router lands on the animated 404 chicken. 5xx and anything else also
        // routes through the SPA, which then renders its own error component.
        int code = status instanceof Integer ? (Integer) status : 404;
        if (code == 401) {
            return "redirect:" + DEFAULT_LOCALE_PREFIX + "/auth/login";
        }
        return "forward:" + DEFAULT_LOCALE_PREFIX + "/index.html";
    }
}
