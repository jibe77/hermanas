package org.jibe77.hermanas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/info")
public class InfoRestController {

    BuildProperties buildProperties;

    public InfoRestController() {
       // default constructor, when build properties are not available.
    }

    @Autowired(required = false)
    public InfoRestController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping
    public BuildProperties version() {
        return buildProperties;
    }

}
