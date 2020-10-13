package org.jibe77.hermanas.controller.music;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ProcessLauncher {

    public Process launch(List<String> commandWithParamsList) throws IOException {
        return new ProcessBuilder(commandWithParamsList).start();
    }

    public Process launch(String ... commandWithParams) throws IOException {
        return new ProcessBuilder(commandWithParams).start();
    }
}
