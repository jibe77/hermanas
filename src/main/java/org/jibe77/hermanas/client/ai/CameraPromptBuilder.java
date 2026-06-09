package org.jibe77.hermanas.client.ai;

/**
 * Builds the chicken-coop analysis prompt sent to the multimodal model.
 *
 * <p>The prompt itself stays in English on purpose — the model's vision and
 * counting accuracy degrade noticeably when the instructions are translated.
 * A small directive is appended to ask the model to answer in the visitor's
 * UI language instead, which keeps the rendered result aligned with the rest
 * of the SPA without paying any quality cost on the questions themselves.</p>
 */
final class CameraPromptBuilder {

    /** Base prompt — kept in a static field so it is not rebuilt on every call. */
    static final String BASE_PROMPT =
            "Can you analyze this pictures from the inside of my chicken coop ?\n"
            + "*   how many chicken can you see ? (watch out, some chicken are black so they can be"
            + " difficult to notice, if you see a tiny glimpse of another chicken then take it in"
            + " account)\n"
            + "*   how many eggs can you see on the floor ? (watch out, an egg is different from a"
            + " poop, even if they are both round oval, a poop is dark and sometimes white, but an"
            + " egg is cuckoo maran. If you see a round dark shape in the shadows of the nesting"
            + " box, verify if it has the smooth, glossy texture of an egg before counting it. If"
            + " it's blue, it's not an egg. Verify it's not a shadow) (note : if you see something"
            + " blue/green it's probably a glove, skip it)\n"
            + "*   is they enough hay on the ground  ?\n"
            + "*   is the door on the lower left corner opened or closed ? (when the door is"
            + " closed, it has a wooden color, when it's opened you can see the outside) (Note: If"
            + " it is night time, an opened door will appear as a dark void or shadow similar to"
            + " the outside, whereas a closed door will show the visible wooden texture/panel of"
            + " the door itself).\n"
            + "*   is there poop / dirt on the floor ? (grade from 1 to 5)\n"
            + "*   there is a fan on the upper right corner, is it dusty ?";

    private CameraPromptBuilder() {
        // Static helper, not meant to be instantiated.
    }

    /**
     * Returns the full prompt to send to the model. {@code lang} is the
     * two-letter ISO code of the UI language ("en", "fr", "ro"); anything
     * else falls back to English, since the model would otherwise pick an
     * unpredictable default.
     */
    static String buildPrompt(String lang) {
        return BASE_PROMPT + "\n\n" + languageInstruction(lang);
    }

    private static String languageInstruction(String lang) {
        String normalized = lang == null ? "en" : lang.trim().toLowerCase();
        switch (normalized) {
            case "fr":
                return "Please answer in French (réponds en français).";
            case "ro":
                return "Please answer in Romanian (răspunde în limba română).";
            default:
                return "Please answer in English.";
        }
    }
}
