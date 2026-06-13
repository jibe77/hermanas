package org.jibe77.hermanas.service.music;

import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.service.ProcessLauncher;
import org.jibe77.hermanas.service.abstract_model.Status;
import org.jibe77.hermanas.service.abstract_model.StatusEnum;
import org.jibe77.hermanas.service.config.ConfigService;
import org.jibe77.hermanas.service.energy.SoundCardService;
import org.jibe77.hermanas.service.event.EventService;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class MusicService {

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

    @Value("${music.enabled}")
    private boolean musicEnabled;

    private ConfigService configService;

    ProcessLauncher processLauncher;

    private Process currentMusicProcess;

    private ConsumptionModeController consumptionModeController;

    private Timer musicSecurityStopTimer;

    private SoundCardService soundCardService;

    private NotificationController notificationController;

    private final EventService eventService;

    private static final Logger logger = LoggerFactory.getLogger(MusicService.class);

    public MusicService(ProcessLauncher processLauncher, ConsumptionModeController consumptionModeController,
                           SoundCardService soundCardService, NotificationController notificationController,
                           ConfigService configService, EventService eventService) {
        this.processLauncher = processLauncher;
        this.consumptionModeController = consumptionModeController;
        this.soundCardService = soundCardService;
        this.notificationController = notificationController;
        this.configService = configService;
        this.eventService = eventService;
    }

    public boolean playMusicRandomly() {
        return playMusicRandomly(configService.getSelectedPlaylist());
    }

    /**
     * Plays the given playlist (sub-directory of {@code music.path.mix}) randomly.
     * If {@code playlist} is null or empty, falls back to the songs sitting at the root
     * of {@code music.path.mix} (legacy layout).
     *
     * @param playlist playlist name (sub-directory), or null/empty for the root of mix
     * @return true if the player was started successfully
     */
    public boolean playMusicRandomly(String playlist) {
        if (!musicEnabled) {
            return false;
        }
        try {
            stop();
            setMusicLevel(configService.getMusicVolumeRegular());
            List<String> listOfFile = getSongFiles(playlist);
            if (listOfFile.isEmpty()) {
                logger.warn("No song found for playlist '{}', music will not start.", playlist);
                return false;
            }
            playMusic(listOfFile);
            return true;
        } catch (IOException e) {
            logger.error("Can't play music.", e);
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

    /**
     * Launch VLC command. Warning : VLC is not supposed to be launched as root but Pi4j
     * requires the application to be launched as root.
     * TODO : run the following command before starting vlc
     *  sed -i 's/geteuid/getppid/g' /usr/bin/vlc
     * otherwise run this command each time VLC is updated.
     * @param listOfFile
     * @param duration
     * @throws IOException
     */
    private void playMusic(List<String> listOfFile, long duration) throws IOException {
        logger.info("Play music with command {} {} {}  {}.",
                musicPlayerStartCmd, musicPlayerNoDispParam
                , musicPlayerShuffle, listOfFile);
        soundCardService.turnOn();
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

    /**
     * Lists the available playlists, i.e. the immediate sub-directories of
     * {@code music.path.mix}. The returned names are sorted alphabetically.
     *
     * @return playlist names (never null)
     */
    public List<String> listPlaylists() {
        File folder = new File(pathToFolder);
        File[] children = folder.listFiles(File::isDirectory);
        if (children == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(children)
                .map(File::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /**
     * Lists the song filenames of the given playlist. Only the file names are
     * returned (not absolute paths) so the API never leaks the host layout.
     *
     * @param playlist playlist name (must be a direct sub-directory of music.path.mix)
     * @return song filenames sorted alphabetically (never null)
     * @throws IllegalArgumentException if the playlist name is invalid or escapes the base directory
     */
    public List<String> listSongs(String playlist) {
        Path playlistDir = resolvePlaylistSafe(playlist);
        File[] children = playlistDir.toFile().listFiles(f -> f.isFile() && isAudio(f.getName()));
        if (children == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(children)
                .map(File::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

    /**
     * Resolves the absolute paths of every song to feed VLC for the given playlist.
     * If {@code playlist} is null or empty, returns the files at the root of mix/
     * (legacy layout).
     */
    private List<String> getSongFiles(String playlist) {
        File folder;
        if (playlist == null || playlist.trim().isEmpty()) {
            folder = new File(pathToFolder);
        } else {
            folder = resolvePlaylistSafe(playlist).toFile();
        }
        File[] files = folder.listFiles(f -> f.isFile() && isAudio(f.getName()));
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files)
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }

    /**
     * Resolves a playlist name to its absolute Path while preventing path traversal.
     *
     * @throws IllegalArgumentException if the name contains separators or escapes mix/
     */
    private Path resolvePlaylistSafe(String playlist) {
        if (playlist == null || playlist.trim().isEmpty()) {
            throw new IllegalArgumentException("Playlist name cannot be empty");
        }
        if (playlist.contains("/") || playlist.contains("\\") || playlist.contains("..")) {
            throw new IllegalArgumentException("Invalid playlist name: " + playlist);
        }
        Path base = Paths.get(pathToFolder).toAbsolutePath().normalize();
        Path target = base.resolve(playlist).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("Playlist resolves outside of base folder: " + playlist);
        }
        if (!target.toFile().isDirectory()) {
            throw new IllegalArgumentException("Playlist does not exist: " + playlist);
        }
        return target;
    }

    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp3", "ogg", "wav", "flac", "m4a", "aac", "wma", "opus"));

    private boolean isAudio(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return false;
        }
        return AUDIO_EXTENSIONS.contains(filename.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private void startSecurityTimer(long durationParam) {
        if (musicSecurityStopTimer != null) {
            musicSecurityStopTimer.cancel();
        }
        musicSecurityStopTimer = new Timer("Music security stop");
        final long duration = durationParam >= 0 ?
                durationParam :
                consumptionModeController.getDuration(
                        configService.getLightSecurityTimerDelayEco(),
                        configService.getLightSecurityTimerDelayRegular(),
                        configService.getLightSecurityTimerDelaySunny(),
                        LocalDateTime.now());
        musicSecurityStopTimer.schedule(new TimerTask() {
                                            public void run() {
                                                logger.info("stopping music after {} ms.", duration);
                                                stop();
                                                // Journal the auto stop here — the timer task fires
                                                // on a non-HTTP thread so EventService.record() also
                                                // captures triggered_by=null.
                                                eventService.recordAuto(EventType.MUSIC_STOPPED, "auto: security timer");
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
            soundCardService.turnOff();
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
                currentMusicProcess == null,
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
        return switcher(param, null);
    }

    /**
     * Switches the music player on/off. When turning on, plays the given playlist;
     * if {@code playlist} is null, uses the currently selected playlist from configuration.
     *
     * @param param true to play, false to stop
     * @param playlist optional playlist name to play (overrides current selection for this call only)
     * @return resulting player status
     */
    public Status switcher(boolean param, String playlist) {
        if (param) {
            if (playlist != null && !playlist.trim().isEmpty()) {
                playMusicRandomly(playlist);
            } else {
                playMusicRandomly();
            }
        } else {
            stop();
        }
        return getStatus();
    }
}
