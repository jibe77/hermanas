package org.jibe77.hermanas.controller.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    Process process;

    Logger logger = LoggerFactory.getLogger(MusicController.class);

    public boolean playSongRandomly() {
        File mixFolder = new File(pathToFolder);
        File[] songsAvailable = mixFolder.listFiles();
        File pickedSong = pickSong(songsAvailable);
        return readMusicFile(pickedSong);
    }

    private File pickSong(File[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }

    public void stop() {
        if (process != null) {
            logger.info("Stop music detroying process.");
            process.destroyForcibly();
        } else {
            logger.info("No music process to stop.");
        }

    }

    public boolean cocorico() {
        return readMusicFile(new File(cocoricoFile));
    }

    private boolean readMusicFile(File musicFile) {
        stop();
        String path = musicFile.getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder(musicPlayerStartCmd, path);
        logger.info("Play music with command {} {}.", musicPlayerStartCmd, path);
        try {
            process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            logger.info("music player says : {}", output);
        } catch (IOException e) {
            logger.error("Can't play music with command {} on file {}.", musicPlayerStartCmd, path);
            return false;
        }
        logger.info("read music method is returning true, everything seems fine.");
        return true;
    }
}
