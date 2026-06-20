package org.jibe77.hermanas.service.capture;

/**
 * Bounding box returned by the vision model for one detected object on the
 * chicken-coop snapshot. Coordinates are normalized (0..1) relative to the
 * image width and height, which lets the SPA overlay them on the {@code <img>}
 * element without having to know the snapshot resolution.
 *
 * <p>{@code type} is the canonical English label ({@code "chicken"} /
 * {@code "egg"}) so the overlay code can pick a color (red / blue) regardless
 * of the UI language the analysis text was rendered in. {@code confidence} is
 * the model's self-reported probability (0..1), used to vary the stroke shade.
 * </p>
 */
public class DetectionDto {

    private final String type;
    private final double confidence;
    private final double x;
    private final double y;
    private final double width;
    private final double height;

    public DetectionDto(String type, double confidence,
                        double x, double y, double width, double height) {
        this.type = type;
        this.confidence = confidence;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String getType() { return type; }
    public double getConfidence() { return confidence; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
}
