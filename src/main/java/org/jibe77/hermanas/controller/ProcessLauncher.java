package org.jibe77.hermanas.controller;

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

    Logger logger = LoggerFactory.getLogger(ProcessLauncher.class);

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
                            logger.debug(line);
                        }
                    } catch (IOException e) {
                        logger.error("can't read process errors.", e);
                        break;
                    }
                } while (line != null);
                logger.info("process error stream is finished (debug only).");
            }).start();
        } else {
            logger.info("error stream is null.");
        }
    }
}
