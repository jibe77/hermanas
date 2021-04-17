package org.jibe77.hermanas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoService {

    BuildProperties buildProperties;

    public InfoService() {
       // default constructor, when build properties are not available.
    }

    @Autowired(required = false)
    public InfoService(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping(value = "/info")
    public BuildProperties version() {
        return buildProperties;
    }

}
