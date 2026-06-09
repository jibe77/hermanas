package org.jibe77.hermanas.service.capture;

/**
 * Lifecycle states of an asynchronous capture pipeline:
 * <ol>
 *   <li>{@link #CAPTURING} — the Pi is grabbing the picture (slow, 5-30 s);</li>
 *   <li>{@link #ANALYZING} — the picture is in transit to the inference server;</li>
 *   <li>{@link #DONE} — image and analysis text are both available;</li>
 *   <li>{@link #ERROR} — the pipeline aborted; {@code errorCode} / {@code errorMessage}
 *       on {@link CaptureStateDto} hold the upstream details.</li>
 * </ol>
 *
 * <p>The image becomes downloadable as soon as the state leaves
 * {@code CAPTURING}, so the SPA can render it while analysis is still running.</p>
 */
public enum CaptureStatus {
    CAPTURING,
    ANALYZING,
    DONE,
    ERROR
}
