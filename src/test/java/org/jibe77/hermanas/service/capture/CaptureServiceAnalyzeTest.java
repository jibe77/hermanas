package org.jibe77.hermanas.service.capture;

import org.jibe77.hermanas.client.ai.AiVisionCache;
import org.jibe77.hermanas.client.ai.AiVisionClient;
import org.jibe77.hermanas.service.camera.CameraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'analyse IA ne doit partir que si on la demande.
 *
 * <p><b>Pourquoi.</b> La page Webcam lançait le pipeline complet — photo *et*
 * analyse — à chaque ouverture. Le serveur d'inférence se retrouvait chargé de
 * travail que personne n'avait réclamé, et beaucoup de ces analyses n'aboutissaient
 * jamais. Elle ne demande plus que la photo ; l'analyse passe par un bouton.</p>
 */
class CaptureServiceAnalyzeTest {

    @TempDir
    Path tempDir;

    private AiVisionClient aiVisionClient;
    private CaptureService captureService;

    @BeforeEach
    void setUp() throws Exception {
        File picture = tempDir.resolve("snapshot.jpg").toFile();
        Files.write(picture.toPath(), new byte[] {1, 2, 3});

        CameraService cameraService = mock(CameraService.class);
        when(cameraService.takePictureCached(anyBoolean(), anyBoolean())).thenReturn(picture);

        aiVisionClient = mock(AiVisionClient.class);
        // Cache vide : sans stub, un « hit » masquerait l'appel au client.
        AiVisionCache cache = mock(AiVisionCache.class);
        when(cache.get(anyString())).thenReturn(null);

        DetectionParser parser = mock(DetectionParser.class);
        when(parser.parse(any())).thenReturn(new DetectionParser.Parsed("texte", java.util.List.of()));

        captureService = new CaptureService(cameraService, aiVisionClient, cache, parser);
    }

    @Test
    @DisplayName("analyze=false : la photo est prise, le serveur d'inférence n'est pas sollicité")
    void doesNotCallTheInferenceServerWhenAnalysisIsNotRequested() throws Exception {
        String id = captureService.startAsync("fr", false);

        verify(aiVisionClient, never()).analyze(any(), anyString());
        // La capture aboutit malgré tout : le client ne doit pas rester en attente
        // d'un état ANALYZING qui ne viendrait jamais.
        assertEquals(CaptureStatus.DONE,
                captureService.getStatus(id).orElseThrow().getStatus(),
                "une capture sans analyse doit se terminer sur DONE");
    }

    @Test
    @DisplayName("analyze=true : l'analyse est bien déclenchée")
    void callsTheInferenceServerWhenAnalysisIsRequested() throws Exception {
        when(aiVisionClient.analyze(any(), anyString())).thenReturn("2 poules, 1 oeuf");

        captureService.startAsync("fr", true);

        verify(aiVisionClient).analyze(any(), anyString());
    }

    @Test
    @DisplayName("L'interrupteur global coupe l'analyse même si on la demande")
    void globalToggleWinsOverAnExplicitRequest() throws Exception {
        // AiVisionClient refuse au niveau du client : c'est le seul passage vers le
        // serveur d'inférence, donc aucun appelant ne peut contourner l'interrupteur.
        when(aiVisionClient.analyze(any(), anyString()))
                .thenThrow(new org.jibe77.hermanas.client.ai.AiVisionException(
                        "AI_DISABLED", "AI features are disabled in the configuration."));

        String id = captureService.startAsync("fr", true);

        // La photo reste disponible : couper l'IA ne doit pas priver de l'image.
        assertEquals(CaptureStatus.ERROR,
                captureService.getStatus(id).orElseThrow().getStatus(),
                "l'analyse refusée doit se solder par une erreur explicite, pas un blocage");
    }

    @Test
    @DisplayName("Sans le drapeau, le comportement historique est conservé")
    void keepsAnalysingByDefaultForExistingCallers() throws Exception {
        when(aiVisionClient.analyze(any(), anyString())).thenReturn("2 poules, 1 oeuf");

        captureService.startAsync("fr");

        verify(aiVisionClient).analyze(any(), anyString());
    }
}
