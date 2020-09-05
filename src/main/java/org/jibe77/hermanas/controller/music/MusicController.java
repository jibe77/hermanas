package org.jibe77.hermanas.controller.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MusicController {

    @Value("${music.player.start.cmd}")
    private String musicPlayerStartCmd;

    @Value("${music.player.suffle.param}")
    private String musicPlayerShuffleParam;

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

    Process currentMusicProcess;

    Logger logger = LoggerFactory.getLogger(MusicController.class);

    public boolean playMusicRandomly() {
        stop();
        try {
            setMusicLevel(volumeLevelRegular);
            List<String> listOfFile = getListOfFiles(pathToFolder);
            logger.info("Play music with command {} {} {}.", musicPlayerStartCmd, musicPlayerShuffleParam, listOfFile);
            List<String> commandWithParams = new ArrayList<>(listOfFile.size() + 2);
            commandWithParams.add(musicPlayerStartCmd);
            commandWithParams.add(musicPlayerShuffleParam);
            commandWithParams.addAll(listOfFile);
            currentMusicProcess = new ProcessBuilder(commandWithParams).start();
        } catch (IOException e) {
            logger.error("Can't play music.", e);
            return false;
        }
        return true;
    }

    private List<String> getListOfFiles(String pathToFolder) {
        File folder = new File(pathToFolder);
        List<File> filesList = Arrays.asList(folder.listFiles());
        Collections.shuffle(filesList);
        return filesList.stream()
                .map(File::getAbsolutePath).collect(Collectors.toList());
    }

    private File pickSong(File[] array) {
        int rnd = new SecureRandom().nextInt(array.length);
        return array[rnd];
    }

    public void stop() {
        if (currentMusicProcess != null) {
            logger.info("Stop music destroying process.");
            currentMusicProcess.destroyForcibly();
            currentMusicProcess = null;
        }
    }

    public boolean cocorico() {
        stop();
        try {
            setMusicLevel(volumeLevelMax);
            File mixFolder = new File(pathToRooster);
            File[] filesAvailable = mixFolder.listFiles();
            File pickedFile = pickSong(filesAvailable);
            return readMusicFile(pickedFile);
        } catch (IOException e) {
            logger.error("Can't play cocorico.", e);
            return false;
        }
    }

    private boolean readMusicFile(File musicFile) {
        String path = musicFile.getAbsolutePath();
        try {
            logger.info("Play music with command {} {}.", musicPlayerStartCmd, path);
            currentMusicProcess = new ProcessBuilder(musicPlayerStartCmd, path).start();
        } catch (IOException e) {
            logger.error("Can't play music with command {} on file {}.", musicPlayerStartCmd, path);
            return false;
        }
        logger.info("read music method is returning true, everything seems fine.");
        return true;
    }

    private void setMusicLevel(String volumeLevel) throws IOException {
        logger.info("Set music level to {} with command {} {} {} {}.",
                volumeLevel,
                volumeCmd,
                volumeCmdArg1,
                volumeCmdArg2,
                volumeLevel);
        new ProcessBuilder(volumeCmd, volumeCmdArg1, volumeCmdArg2, volumeLevel).start();
    }
}