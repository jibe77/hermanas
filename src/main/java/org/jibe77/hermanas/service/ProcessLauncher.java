package org.jibe77.hermanas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

@Component
public class ProcessLauncher {

    private static final Logger logger = LoggerFactory.getLogger(ProcessLauncher.class);

    public Process launch(List<String> commandWithParamsList) throws IOException {
        return new ProcessBuilder(commandWithParamsList).start();
    }

    public Process launch(String ... commandWithParams) throws IOException {
        return new ProcessBuilder(commandWithParams).start();
    }

    public String launchAndReturnResult(String ... comandWithParams) throws IOException {
        Process process = new ProcessBuilder(comandWithParams).start();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString().trim();
    }

    public void printErrorStreamInThread(Process currentStreamingProcess) {
        printErrorStreamInThread(currentStreamingProcess, "process", false);
    }

    /**
     * Drains the error stream of a launched process in a dedicated thread.
     *
     * @param label name used in the log lines, so several processes can be told apart
     * @param warn  when true, the output is logged at WARN instead of DEBUG. Used for
     *              {@code mjpg_streamer}: at DEBUG the {@code No such file or directory}
     *              raised by bash never reaches spring.log, and a dead streamer is only
     *              visible much later as an unexplained exit value.
     */
    public void printErrorStreamInThread(Process currentStreamingProcess, String label, boolean warn) {
        InputStream errorStream = currentStreamingProcess.getErrorStream();
        if (errorStream != null) {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(errorStream));
            new Thread(() -> {
                String line;
                logger.info("error stream is opened (debug only)...");
                do {
                    try {
                        line = bufferedReader.readLine();
                        if (line != null) {
                            if (warn) {
                                logger.warn("{}: {}", label, line);
                            } else {
                                logger.debug(line);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("can't read process errors.", e);
                        break;
                    }
                } while (line != null);
                logger.info("process error stream is finished (debug only).");
                if (warn) {
                    // La valeur de sortie est remontée dès que stderr se ferme, sans
                    // attendre qu'un stopStream la découvre plusieurs minutes plus tard.
                    try {
                        logger.warn("{} exited with value {}.", label, currentStreamingProcess.exitValue());
                    } catch (IllegalThreadStateException stillRunning) {
                        logger.debug("{} error stream closed while the process was still alive.", label);
                    }
                }
            }, label + " error stream").start();
        } else {
            logger.info("error stream is null.");
        }
    }
}
