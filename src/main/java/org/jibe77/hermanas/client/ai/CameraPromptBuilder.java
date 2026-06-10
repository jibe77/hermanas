package org.jibe77.hermanas.client.ai;

import org.jibe77.hermanas.service.config.ConfigService;
import org.springframework.stereotype.Component;

/**
 * Builds the chicken-coop analysis prompt sent to the multimodal model.
 *
 * <p>Two sources for the base prompt:
 * <ul>
 *   <li>the built-in {@link #DEFAULT_PROMPT} below — used by default;</li>
 *   <li>a runtime override stored via {@code ConfigService.setAiInferencePrompt}
 *       — empty means "fall back to the default".</li>
 * </ul>
 * In both cases the prompt itself stays in English on purpose — the model's
 * vision and counting accuracy degrade noticeably when the instructions are
 * translated. A small directive is appended to ask the model to answer in the
 * visitor's UI language, which keeps the rendered result aligned with the
 * rest of the SPA without paying any quality cost on the questions
 * themselves.</p>
 */
@Component
public class CameraPromptBuilder {

    /**
     * Default prompt — kept in a constant so the SPA can pre-fill the admin
     * panel with it when the operator has never overridden it.
     */
    public static final String DEFAULT_PROMPT =
            "Can you analyze this pictures from the inside of my chicken coop ? Be very"
            + " straightforward in your answers, and don't reply like it's questions, make real"
            + " answer because the person who will read this text can't see the prompt.\n"
            + "*   how many chicken can you see ? (watch out, some chicken are black so they can be"
            + " difficult to notice, if you see a tiny glimpse of another chicken then take it in"
            + " account, if this is just a silhouette or a shadow it's a false positive so skip"
            + " it) However, be very strict: if a black shape looks like a shadow, a dark piece of"
            + " wood, or a pile of feathers without a distinct head or eye, do not count it. If"
            + " you are not 100% sure it is a living bird, assume it is a shadow.\n"
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

    private final ConfigService configService;

    public CameraPromptBuilder(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * Returns the full prompt to send to the model. {@code lang} is the
     * two-letter ISO code of the UI language ("en", "fr", "ro"); anything
     * else falls back to English, since the model would otherwise pick an
     * unpredictable default.
     */
    public String buildPrompt(String lang) {
        return resolveBase() + "\n\n" + languageInstruction(lang);
    }

    /**
     * Returns the strict language directive meant to be sent as a {@code system}
     * message. Vision models (notably qwen2.5-vl) ignore a single user-level
     * language hint roughly one call out of three; restating the constraint in a
     * dedicated system message — which carries higher steering weight — is what
     * keeps the output consistently in the requested locale.
     */
    public String buildSystemInstruction(String lang) {
        String normalized = lang == null ? "en" : lang.trim().toLowerCase();
        switch (normalized) {
            case "fr":
                return "Tu es un assistant qui répond EXCLUSIVEMENT en français."
                        + " Toute réponse contenant le moindre mot anglais est considérée"
                        + " comme une erreur critique. Ne traduis pas la consigne, ne"
                        + " l'explique pas : applique-la silencieusement.";
            case "ro":
                return "Ești un asistent care răspunde EXCLUSIV în limba română."
                        + " Orice răspuns care conține fie și un singur cuvânt în engleză"
                        + " este considerat o eroare critică. Nu traduce instrucțiunea,"
                        + " nu o explica: aplic-o în tăcere.";
            default:
                return "You are an assistant that answers exclusively in English.";
        }
    }

    private String resolveBase() {
        String configured = configService.getAiInferencePrompt();
        if (configured == null || configured.trim().isEmpty()) {
            return DEFAULT_PROMPT;
        }
        return configured;
    }

    private static String languageInstruction(String lang) {
        String normalized = lang == null ? "en" : lang.trim().toLowerCase();
        switch (normalized) {
            case "fr":
                // Doubled-up directive: a short parenthetical hint alone is
                // routinely ignored by qwen2.5-vl (the answer comes back in
                // English). Restating the constraint in plain French + asking
                // the model to start its first sentence in French is what made
                // it stick during testing.
                return "IMPORTANT: Write your entire answer in French. Do not use any English."
                        + " Tu dois rédiger TOUTE ta réponse en français, sans aucun mot d'anglais."
                        + " Commence ta première phrase en français.";
            case "ro":
                return "IMPORTANT: Write your entire answer in Romanian. Do not use any English."
                        + " Trebuie să scrii ÎNTREAGA ta răspundere în limba română, fără cuvinte"
                        + " în engleză. Începe prima ta propoziție în română.";
            default:
                return "Please answer in English.";
        }
    }
}
