package org.jibe77.hermanas.controller.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Random;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MusicController {

    @Value("${music.path.mix}")
    private String pathToFolder;

    @Value("${music.path.cocorico}")
    private String cocoricoFile;

    private Clip clip;

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
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.close();
        }
    }

    public boolean cocorico() {
        return readMusicFile(new File(cocoricoFile));
    }

    private boolean readMusicFile(File musicFile) {
        AudioInputStream audioInputStream;
        logger.info("Reading music file {}", musicFile.getAbsolutePath());
        try {

            // create AudioInputStream object
            audioInputStream =
                    AudioSystem.getAudioInputStream(musicFile);

            // stop pre-exisiting running song
            stop();

            // create clip reference
            clip = AudioSystem.getClip();

            // open audioInputStream to the clip
            clip.open(audioInputStream);
            clip.start();
            return true;
        } catch (UnsupportedAudioFileException e) {
            logger.error("Error playing file due to unsupported file.", e);
            return false;
        } catch (IOException e) {
            logger.error("Error playing music.", e);
            return false;
        } catch (LineUnavailableException e) {
            logger.error("Error playing clip.", e);
            return false;
        }
    }
}
