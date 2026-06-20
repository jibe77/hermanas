package org.jibe77.hermanas.service.capture;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionParserTest {

    private final DetectionParser parser = new DetectionParser();

    @Test
    void parsesClassicFencedBlock() {
        String raw = "Three hens visible.\n\n"
                + "```json\n"
                + "{\"detections\":[{\"type\":\"chicken\",\"confidence\":0.9,\"box\":[0.1,0.2,0.3,0.4]}]}\n"
                + "```";
        DetectionParser.Parsed p = parser.parse(raw);
        assertEquals("Three hens visible.", p.humanText());
        assertEquals(1, p.detections().size());
        assertEquals("chicken", p.detections().get(0).getType());
    }

    /**
     * Real output we captured from the running model: closing fence missing,
     * blank line between the opener and the JSON, prose after "Rules for the
     * JSON block:" leaked from the prompt back into the answer. The human text
     * must lose every trace of the JSON block; the boxes still parse.
     */
    @Test
    void parsesRealOutputWithMissingCloseFence() {
        String raw = "On peut voir trois poules dans le poulailler. Il n'y a aucun œuf"
                + " visible sur le sol. La quantité de foin au sol est suffisante. La porte"
                + " située en bas à gauche est ouverte. Le niveau de saleté sur le sol est"
                + " de 2 sur 5. Le ventilateur en haut à droite semble légèrement"
                + " poussiéreux.\n\n"
                + "```json\n\n"
                + "{\"detections\":[\n\n"
                + "{\"type\":\"chicken\",\"confidence\":0.95,\"box\":[0.18,0.05,0.65,0.85]},\n\n"
                + "{\"type\":\"chicken\",\"confidence\":0.88,\"box\":[0.35,0.12,0.52,0.35]},\n\n"
                + "{\"type\":\"chicken\",\"confidence\":0.82,\"box\":[0.48,0.15,0.62,0.38]}\n\n"
                + "]}";
        DetectionParser.Parsed p = parser.parse(raw);
        assertFalse(p.humanText().contains("json"),
                "human text leaked the JSON fence: " + p.humanText());
        assertFalse(p.humanText().contains("\"detections\""),
                "human text leaked the detections array: " + p.humanText());
        assertFalse(p.humanText().contains("```"),
                "human text leaked the fence marker: " + p.humanText());
        assertTrue(p.humanText().startsWith("On peut voir trois poules"));
        assertTrue(p.humanText().endsWith("poussiéreux."));
        assertEquals(3, p.detections().size());
    }

    @Test
    void parsesUnfencedTrailingJson() {
        String raw = "One egg.\n\n"
                + "{\"detections\":[{\"type\":\"egg\",\"confidence\":0.7,\"box\":[0.5,0.5,0.05,0.04]}]}";
        DetectionParser.Parsed p = parser.parse(raw);
        assertEquals("One egg.", p.humanText());
        assertEquals(1, p.detections().size());
        assertEquals("egg", p.detections().get(0).getType());
    }

    @Test
    void returnsEmptyDetectionsWhenJsonInvalid() {
        String raw = "Nothing here.\n\n```json\n{not valid}\n```";
        DetectionParser.Parsed p = parser.parse(raw);
        assertEquals("Nothing here.", p.humanText());
        assertTrue(p.detections().isEmpty());
    }

    @Test
    void returnsRawTextWhenNoBlock() {
        String raw = "No detections at all in this answer.";
        DetectionParser.Parsed p = parser.parse(raw);
        assertEquals(raw, p.humanText());
        assertTrue(p.detections().isEmpty());
    }

    @Test
    void clampsCoordinatesAndConfidence() {
        String raw = "```json\n"
                + "{\"detections\":[{\"type\":\"chicken\",\"confidence\":1.5,\"box\":[-0.1,2.0,0.3,0.4]}]}\n"
                + "```";
        DetectionParser.Parsed p = parser.parse(raw);
        assertEquals(1, p.detections().size());
        DetectionDto d = p.detections().get(0);
        assertEquals(1.0, d.getConfidence(), 1e-9);
        assertEquals(0.0, d.getX(), 1e-9);
        assertEquals(1.0, d.getY(), 1e-9);
    }

    @Test
    void skipsZeroSizedBoxes() {
        String raw = "```json\n"
                + "{\"detections\":["
                + "{\"type\":\"egg\",\"confidence\":0.5,\"box\":[0.1,0.1,0,0.2]},"
                + "{\"type\":\"egg\",\"confidence\":0.5,\"box\":[0.2,0.2,0.05,0.05]}"
                + "]}\n"
                + "```";
        List<DetectionDto> ds = parser.parse(raw).detections();
        assertEquals(1, ds.size());
    }

    @Test
    void handlesEmptyInput() {
        DetectionParser.Parsed p = parser.parse("");
        assertEquals("", p.humanText());
        assertTrue(p.detections().isEmpty());
    }
}
