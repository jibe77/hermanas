package org.jibe77.hermanas.service;

import org.jibe77.hermanas.controller.config.ConfigController;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoService {

    BuildProperties buildProperties;

    public InfoService(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping(value = "/info/version")
    public String version() {
        return buildProperties.getVersion();
    }
}
