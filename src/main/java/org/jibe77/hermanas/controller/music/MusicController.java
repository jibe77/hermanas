package org.jibe77.hermanas.controller.music;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import org.jibe77.hermanas.controller.ProcessLauncher;
import org.jibe77.hermanas.controller.gpio.GpioHermanasController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MusicController {

    @Value("${music.player.start.cmd}")
    private String musicPlayerStartCmd;

    @Value("${music.player.nodisp.param}")
    private String musicPlayerNoDispParam;

    @Value("${music.player.shuffle.param}")
    private String musicPlayerShuffle;

    @Value("${music.path.mix}")
    private String pathToFolder;

    @Value("${music.path.rooster}")
    private String pathToRooster;

    @Value("${music.volume.cmd}")
    private String volumeCmd;

    @Value("${music.volume.arg1}")
    private String volumeCmdArg1;

    @Value("${music.volume.arg2}")
    private String volumeCmdArg2;

    @Value("${music.volume.max}")
    private String volumeLevelMax;

    @Value("${music.volume.regular}")
    private String volumeLevelRegular;

    @Value("${music.security.timer.delay.eco}")
    private long musicSecurityTimerDelayEco;

    @Value("${music.security.timer.delay.regular}")
    private long musicSecurityTimerDelayRegular;

    @Value("${music.security.timer.delay.sunny}")
    private long musicSecurityTimerDelaySunny;

    @Value("${music.enabled}")
    private boolean musicEnabled;

    @Value("${music.relay.gpio.address}")
    private int musicRelayGpioAddress;

    ProcessLauncher processLauncher;

    private Process currentMusicProcess;

    private ConsumptionModeManager consumptionModeManager;

    private Timer musicSecurityStopTimer;

    private GpioHermanasController gpioHermanasController;

    private GpioPinDigitalOutput gpioPinDigitalOutput;

    Logger logger = LoggerFactory.getLogger(MusicController.class);

    public MusicController(ProcessLauncher processLauncher, ConsumptionModeManager consumptionModeManager,
                           GpioHermanasController gpioHermanasController) {
        this.processLauncher = processLauncher;
        this.consumptionModeManager = consumptionModeManager;
        this.gpioHermanasController = gpioHermanasController;
    }

    @PostConstruct
    private void init() {
        if (musicEnabled) {
            logger.info("provisioning audio card pin on {}.", musicRelayGpioAddress);
            gpioPinDigitalOutput = gpioHermanasController.provisionOutput(musicRelayGpioAddress);
        }
    }

    public boolean playMusicRandomly() {
        if (musicEnabled) {
            gpioPinDigitalOutput.high();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.info("Interrupted sleep.");
                Thread.currentThread().interrupt();
            }
            stop();
            try {
                setMusicLevel(volumeLevelRegular);
                List<String> listOfFile = getListOfFiles(pathToFolder);
                playMusic(listOfFile);
            } catch (IOException e) {
                logger.error("Can't play music.", e);
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private void playMusic(File musicFile, long duration) throws IOException {
        List<String> listOfFile = new ArrayList<>(1);
        listOfFile.add(musicFile.getAbsolutePath());
        playMusic(listOfFile, duration);
    }

    private void playMusic(List<String> listOfFile) throws IOException {
        playMusic(listOfFile, -1L);
    }

    private void playMusic(List<String> listOfFile, long duration) throws IOException {
        logger.info("Play music with command {} {} {}  {}.",
                musicPlayerStartCmd, musicPlayerNoDispParam
                , musicPlayerShuffle, listOfFile);
        gpioPinDigitalOutput.high();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.info("Interrupted sleep.");
            Thread.currentThread().interrupt();
        }
        List<String> commandWithParams = new ArrayList<>(listOfFile.size() + 3);
        commandWithParams.add(musicPlayerStartCmd);
        commandWithParams.add(musicPlayerNoDispParam);
        commandWithParams.add(musicPlayerShuffle);
        commandWithParams.addAll(listOfFile);
        currentMusicProcess = processLauncher.launch(commandWithParams);
        processLauncher.printErrorStreamInThread(currentMusicProcess);
        startSecurityTimer(duration);
    }

    private List<String> getListOfFiles(String pathToFolder) {
        File folder = new File(pathToFolder);
        List<File> filesList = Arrays.asList(folder.listFiles());
        return filesList.stream()
                .map(File::getAbsolutePath).collect(Collectors.toList());
    }

    private void startSecurityTimer(long durationParam) {
        if (musicSecurityStopTimer != null) {
            musicSecurityStopTimer.cancel();
        }
        musicSecurityStopTimer = new Timer("Music security stop");
        final long duration = durationParam >= 0 ?
                durationParam :
                consumptionModeManager.getDuration(
                    musicSecurityTimerDelayEco, musicSecurityTimerDelayRegular, musicSecurityTimerDelaySunny);
        musicSecurityStopTimer.schedule(new TimerTask() {
                                            public void run() {
                                                logger.info("stopping music after {} ms.", duration);
                                                stop();
                                            }
                                        },
                duration);
    }

    private File pickSong(File[] array) {
        int rnd = new SecureRandom().nextInt(array.length);
        return array[rnd];
    }

    public void stop() {
        if (musicEnabled && currentMusicProcess != null) {
            logger.info("Stop music destroying process.");
            currentMusicProcess.destroyForcibly();
            currentMusicProcess = null;
            if (musicSecurityStopTimer != null) {
                musicSecurityStopTimer.cancel();
                musicSecurityStopTimer = null;
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.info("Interrupted sleep.");
                Thread.currentThread().interrupt();
            }
            gpioPinDigitalOutput.low();
        }
    }

    public boolean cocorico() {
        if (musicEnabled) {
            logger.info("Play cocorico !");
            stop();
            try {
                setMusicLevel(volumeLevelMax);
                File mixFolder = new File(pathToRooster);
                File[] filesAvailable = mixFolder.listFiles();
                File pickedFile = pickSong(filesAvailable);
                playMusic(pickedFile, 30000L);
                return true;
            } catch (IOException e) {
                logger.error("Can't play cocorico.", e);
                return false;
            }
        } else {
            return false;
        }
    }

    @PreDestroy
    private void tearDown() {
        if (musicEnabled) {
            stop();
        }
    }

    /**
     * This methods returns true if music is playing
     *
     * @return true if music is playing
     */
    public boolean isPlaying() {
        logger.info("status of player is request, current process is null : {} and is alive : {}",
                currentMusicProcess != null,
                currentMusicProcess != null && currentMusicProcess.isAlive());
        return (currentMusicProcess != null && currentMusicProcess.isAlive());
    }

    private void setMusicLevel(String volumeLevel) throws IOException {
        logger.info("Set music level to {} with command {} {} {} {}.",
                volumeLevel,
                volumeCmd,
                volumeCmdArg1,
                volumeCmdArg2,
                volumeLevel);
        processLauncher.launch(volumeCmd, volumeCmdArg1, volumeCmdArg2, volumeLevel);
    }

    Process getCurrentMusicProcess() {
        return currentMusicProcess;
    }

    void setCurrentMusicProcess(Process currentMusicProcess) {
        this.currentMusicProcess = currentMusicProcess;
    }

    protected void setGpioPinDigitalOutput(GpioPinDigitalOutput gpioPinDigitalOutput) {
        this.gpioPinDigitalOutput = gpioPinDigitalOutput;
    }
}
