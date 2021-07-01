package org.jibe77.hermanas.controller.music;

import org.jibe77.hermanas.controller.ProcessLauncher;
import org.jibe77.hermanas.controller.abstract_model.Status;
import org.jibe77.hermanas.controller.abstract_model.StatusEnum;
import org.jibe77.hermanas.controller.energy.SoundCardController;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.jibe77.hermanas.websocket.Appliance;
import org.jibe77.hermanas.websocket.CoopStatus;
import org.jibe77.hermanas.websocket.NotificationController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.*;
import java.security.SecureRandom;
import java.time.LocalDateTime;
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

    ProcessLauncher processLauncher;

    private Process currentMusicProcess;

    private ConsumptionModeController consumptionModeController;

    private Timer musicSecurityStopTimer;

    private SoundCardController soundCardController;

    private NotificationController notificationController;

    Logger logger = LoggerFactory.getLogger(MusicController.class);

    public MusicController(ProcessLauncher processLauncher, ConsumptionModeController consumptionModeController,
                           SoundCardController soundCardController, NotificationController notificationController) {
        this.processLauncher = processLauncher;
        this.consumptionModeController = consumptionModeController;
        this.soundCardController = soundCardController;
        this.notificationController = notificationController;
    }

    public boolean playMusicRandomly() {
        if (musicEnabled) {
            try {
                stop();
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
        soundCardController.turnOn();
        List<String> commandWithParams = new ArrayList<>(listOfFile.size() + 3);
        commandWithParams.add(musicPlayerStartCmd);
        commandWithParams.add(musicPlayerNoDispParam);
        commandWithParams.add(musicPlayerShuffle);
        commandWithParams.addAll(listOfFile);
        currentMusicProcess = processLauncher.launch(commandWithParams);
        processLauncher.printErrorStreamInThread(currentMusicProcess);
        startSecurityTimer(duration);
        notificationController.notify(new CoopStatus(Appliance.MUSIC, StatusEnum.ON));
    }

    private List<String> getListOfFiles(String pathToFolder) {
        File folder = new File(pathToFolder);
        File[] files = folder.listFiles();
        List<File> filesList = files != null ? Arrays.asList(files) : Collections.emptyList();
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
                consumptionModeController.getDuration(
                    musicSecurityTimerDelayEco, musicSecurityTimerDelayRegular, musicSecurityTimerDelaySunny, LocalDateTime.now());
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
            soundCardController.turnOff();
            notificationController.notify(new CoopStatus(Appliance.MUSIC, StatusEnum.OFF));
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
    public Status getStatus() {
        logger.info("status of player is request, current process is null : {} and is alive : {}",
                currentMusicProcess != null,
                currentMusicProcess != null && currentMusicProcess.isAlive());
        return new Status (
                (currentMusicProcess != null && currentMusicProcess.isAlive()) ? StatusEnum.ON : StatusEnum.OFF,
                -1);
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

    public Status switcher(boolean param) {
        if (param) {
            playMusicRandomly();
        } else {
            stop();
        }
        return getStatus();
    }
}
