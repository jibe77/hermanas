package org.jibe77.hermanas.controller.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Random;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MusicController {

    @Value("${music.player.start.cmd}")
    private String musicPlayerStartCmd;

    @Value("${music.path.mix}")
    private String pathToFolder;

    @Value("${music.path.cocorico}")
    private String cocoricoFile;

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

    public boolean playSongRandomly() {
        File mixFolder = new File(pathToFolder);
        File[] songsAvailable = mixFolder.listFiles();
        File pickedSong = pickSong(songsAvailable);
        return readMusicFile(pickedSong, volumeLevelRegular);
    }

    private File pickSong(File[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    public void stop() {
        if (currentMusicProcess != null) {
            logger.info("Stop music detroying process.");
            currentMusicProcess.destroyForcibly();
        } else {
            logger.info("No music process to stop.");
        }
    }

    public boolean cocorico() {
        return readMusicFile(new File(cocoricoFile), volumeLevelMax);
    }

    private boolean readMusicFile(File musicFile, String volumeLevel) {
        stop();

        String path = musicFile.getAbsolutePath();
        try {
            logger.info("Set music level to {} with command {} {} {} {}.",
                    volumeLevel,
                    volumeCmd,
                    volumeCmdArg1,
                    volumeCmdArg2,
                    volumeLevel);
            new ProcessBuilder(volumeCmd, volumeCmdArg1, volumeCmdArg2, volumeLevel).start();

            logger.info("Play music with command {} {}.", musicPlayerStartCmd, path);
            currentMusicProcess = new ProcessBuilder(musicPlayerStartCmd, path).start();
        } catch (IOException e) {
            logger.error("Can't play music with command {} on file {}.", musicPlayerStartCmd, path);
            return false;
        }
        logger.info("read music method is returning true, everything seems fine.");
        return true;
    }
}