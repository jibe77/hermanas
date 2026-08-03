package org.jibe77.hermanas.service.gpio;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.*;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmConfig;
import com.pi4j.io.pwm.PwmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
@Profile("gpio-rpi")
public class GpioHermanasRpiService implements GpioHermanasService {

    private Context pi4j;

    /**
     * Marge accordée au processus de capture au-delà de {@code --timeout} : encodage
     * JPEG et écriture sur la carte SD, lents sur un Zero 2 W et très variables selon
     * la charge.
     */
    private static final int CAPTURE_GRACE_MS = 15000;

    private final CameraConfiguration cameraConfiguration;

    private static final Logger logger = LoggerFactory.getLogger(GpioHermanasRpiService.class);

    @Value("${camera.rpicam.still.path:/usr/bin/rpicam-still}")
    private String rpicamStillPath;

    public GpioHermanasRpiService(CameraConfiguration cameraConfiguration) {
        this.cameraConfiguration = cameraConfiguration;
    }

    @PostConstruct
    private void initialiseGpioPins() {
        logger.info("Initialise GPIO ...");

        // Isolé dans son propre try : une erreur ici ne doit pas empêcher le reste
        // du démarrage. La caméra, elle, n'a plus rien à initialiser — rpicam-still
        // est un exécutable système, invoqué à chaque capture.
        try {
            logger.info("Init pi4j context.");
            pi4j = Pi4J.newAutoContext();
        } catch (Exception | UnsatisfiedLinkError e) {
            logger.error("Can't initialise pi4j context — GPIO will be unavailable.", e);
        }

        logger.info("... initialisation done.");
    }

    /**
     * Capture via {@code rpicam-still}.
     *
     * <p>picam reposait sur MMAL, la pile Broadcom retirée de Raspberry Pi OS arm64
     * (Trixie) au profit de libcamera. Le {@code .so} compilé pour ARMv6/Buster ne
     * peut donc plus fonctionner : ce n'est pas un défaut de chargement, ce qu'il
     * appelle n'existe plus. On délègue à l'outil système, comme le fait déjà le
     * streaming avec {@code ProcessLauncher}.</p>
     *
     * <p>Le timeout du processus est calé sur {@code camera.*.delay} + une marge :
     * {@code --timeout} est le temps laissé à l'auto-exposition avant déclenchement,
     * auquel s'ajoutent l'encodage JPEG et l'écriture, lents sur un Zero 2 W.</p>
     */
    @Override
    public void takePicture(File destination, boolean highQuality) throws IOException {
        int delay = cameraConfiguration.delay(highQuality);
        List<String> command = buildCaptureCommand(destination, highQuality, delay);

        logger.info("Capture via {}", String.join(" ", command));
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException e) {
            throw new IOException("Can't launch " + rpicamStillPath
                    + ". Is rpicam-apps installed ?", e);
        }

        String output;
        boolean finished;
        try (InputStream in = process.getInputStream()) {
            // Lire le flux AVANT waitFor : rpicam-still est bavard et un tube plein
            // bloquerait le processus, provoquant un faux timeout.
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            finished = process.waitFor(delay + CAPTURE_GRACE_MS, MILLISECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("Capture interrupted.", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Capture timed out after " + (delay + CAPTURE_GRACE_MS) + " ms.");
        }
        if (process.exitValue() != 0) {
            throw new IOException("rpicam-still failed (exit " + process.exitValue() + ") : " + output);
        }
        // rpicam-still peut sortir en 0 sans rien écrire si la caméra est occupée.
        if (!destination.isFile() || destination.length() == 0) {
            throw new IOException("rpicam-still reported success but produced no image at "
                    + destination.getAbsolutePath());
        }
        logger.info("Picture captured ({} bytes).", destination.length());
    }

    /**
     * Traduit la configuration Hermanas en options {@code rpicam-still}. Qualité,
     * rotation et luminosité viennent de {@code ConfigService} via
     * {@link CameraConfiguration}, donc un changement à chaud reste pris en compte
     * dès la photo suivante.
     */
    private List<String> buildCaptureCommand(File destination, boolean highQuality, int delay) {
        List<String> command = new ArrayList<>(List.of(
                rpicamStillPath,
                "--output", destination.getAbsolutePath(),
                "--width", Integer.toString(cameraConfiguration.width(highQuality)),
                "--height", Integer.toString(cameraConfiguration.height(highQuality)),
                "--quality", Integer.toString(cameraConfiguration.quality(highQuality)),
                // Délai avant capture : laisse l'auto-exposition converger.
                "--timeout", Integer.toString(delay),
                "--nopreview"));

        // rpicam-still n'accepte que 0/90/180/270 ; toute autre valeur ferait
        // échouer la commande entière.
        int rotation = cameraConfiguration.rotation();
        if (rotation == 90 || rotation == 180 || rotation == 270) {
            command.add("--rotation");
            command.add(Integer.toString(rotation));
        } else if (rotation != 0) {
            logger.warn("Rotation {} non supportée par rpicam-still (0/90/180/270), ignorée.",
                    rotation);
        }

        // La luminosité picam est en 0..100, celle de rpicam-still en -1.0..1.0.
        command.add("--brightness");
        command.add(String.format(Locale.ROOT, "%.2f",
                (cameraConfiguration.brightness() - 50) / 50.0));

        // Zone du capteur lue. rpicam-still recadre PUIS rééchantillonne à la taille
        // de sortie : c'est un zoom numérique. La cohérence entre le ROI et les
        // dimensions demandées relève du réglage, pas du code — l'interface le
        // rappelle et propose la hauteur à saisir.
        String roi = cameraConfiguration.roi();
        if (roi != null && !roi.isBlank()) {
            command.add("--roi");
            command.add(roi.trim());
        }

        // Balance des blancs. Les gains explicites priment : rpicam-still ignore
        // --awb dès que --awbgains est fourni, autant ne pas envoyer les deux.
        String awbGains = cameraConfiguration.awbGains();
        String awb = cameraConfiguration.awb();
        if (awbGains != null && !awbGains.isBlank()) {
            command.add("--awbgains");
            command.add(awbGains.trim());
        } else if (awb != null && !awb.isBlank()) {
            command.add("--awb");
            command.add(awb.trim().toLowerCase(Locale.ROOT));
        }

        return command;
    }

    @PreDestroy
    private void tearDown() {
        // pi4j reste null si son initialisation a échoué. Sans ce garde, l'arrêt
        // du contexte lève une NullPointerException qui masque l'erreur d'origine
        // dans les logs.
        if (pi4j == null) {
            logger.info("No pi4j context to shut down.");
            return;
        }
        logger.info("Shutdown gpio instance.");
        pi4j.shutdown();
    }

    public DigitalInput provisionInput(String id, String name, int gpioAddress) {
        DigitalInputConfigBuilder d = DigitalInput.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .bcm(gpioAddress)
                .pull(PullResistance.PULL_DOWN)
                .debounce(3000L)
                .provider("ffm-digital-input");
        return pi4j.create(d);
    }

    @Override
    public DigitalOutput provisionOutput(String id, String name, int gpioAddress) {
        DigitalOutputConfigBuilder d = DigitalOutput.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .bcm(gpioAddress)
                .initial(DigitalState.LOW)
                .shutdown(DigitalState.LOW)
                .provider("ffm-digital-output");
        DigitalOutput digitalOutput = pi4j.create(d);
        digitalOutput.addListener(event -> {
            logger.info("Event on {} on address {}, state is now {}",
                    event.source().getId(), event.source().bcm(), event.state());
        });
        return digitalOutput;
    }

    /**
     * Correspondance entre broche BCM et canal PWM matériel du BCM2710 (Pi Zero 2 W).
     *
     * <p>Le plugin FFM ne gère que {@link PwmType#HARDWARE} — le PWM logiciel de
     * pi4j 2.x/pigpio n'existe plus — et son provider refuse {@code .bcm()} :
     * « PWM Chip and Channel are needed for hardware PWM with the FFM I/O provider ».
     * Il attend l'adressage sysfs du kernel ({@code /sys/class/pwm/pwmchipN/pwmM}),
     * pas une numérotation GPIO.</p>
     *
     * <p>Le SoC n'expose que deux canaux, chacun accessible depuis deux broches :
     * PWM0 sur GPIO 12 et 18, PWM1 sur GPIO 13 et 19. Toute autre broche — dont
     * GPIO 25, utilisé jusqu'ici — est hors de portée. D'où le recâblage du servo
     * vers GPIO 12 (broche physique 32), GPIO 18 étant déjà pris par
     * {@code door.button.up}.</p>
     */
    private static final Map<Integer, Integer> BCM_TO_PWM_CHANNEL = Map.of(
            12, 0,   // PWM0
            18, 0,   // PWM0 (alternative)
            13, 1,   // PWM1
            19, 1);  // PWM1 (alternative)

    /**
     * Le Pi Zero 2 W n'expose qu'un seul contrôleur PWM, donc chip 0. Les modèles
     * plus récents (Pi 5) en ont plusieurs, d'où la constante nommée plutôt qu'un
     * littéral disséminé.
     */
    private static final int PWM_CHIP = 0;

    public Pwm provisionPwm(String id, String name, int gpioAddress) {
        Integer channel = BCM_TO_PWM_CHANNEL.get(gpioAddress);
        if (channel == null) {
            throw new IllegalArgumentException(
                    "GPIO " + gpioAddress + " n'expose pas de PWM matériel sur ce modèle. "
                    + "Broches possibles : " + BCM_TO_PWM_CHANNEL.keySet()
                    + ". Adapter door.servo.gpio.address et le câblage.");
        }

        logger.info("Provision PWM on BCM {} -> chip {}, channel {}.",
                gpioAddress, PWM_CHIP, channel);

        PwmConfig pwmConfig = Pwm.newConfigBuilder(pi4j)
                .id(id)
                .name(name)
                .chip(PWM_CHIP)
                .channel(channel)
                .pwmType(PwmType.HARDWARE)
                .initial(0)
                .shutdown(0)
                .provider("ffm-pwm")
                .build();
        return pi4j.create(pwmConfig);
    }
}
