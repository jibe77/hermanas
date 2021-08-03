package org.jibe77.hermanas.controller.config;

import org.apache.commons.lang3.StringUtils;
import org.jibe77.hermanas.data.entity.Parameter;
import org.jibe77.hermanas.data.repository.ParameterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class ConfigController {

    @Value("${light.security.timer.delay.eco}")
    private long lightSecurityTimerDelayEco;

    @Value("${light.security.timer.delay.regular}")
    private long lightSecurityTimerDelayRegular;

    @Value("${light.security.timer.delay.sunny}")
    private long lightSecurityTimerDelaySunny;

    @Value("${fan.security.timer.delay.eco}")
    private long fanSecurityTimerDelayEco;

    @Value("${fan.security.timer.delay.regular}")
    private long fanSecurityTimerDelayRegular;

    @Value("${fan.security.timer.delay.sunny}")
    private long fanSecurityTimerDelaySunny;

    @Value("${music.security.timer.delay.eco}")
    private long musicSecurityTimerDelayEco;

    @Value("${music.security.timer.delay.regular}")
    private long musicSecurityTimerDelayRegular;

    @Value("${music.security.timer.delay.sunny}")
    private long musicSecurityTimerDelaySunny;

    @Value("${consumption.mode.eco.days.around.winter.solstice}")
    private int ecoModeNbrDaysAroundWinterSolstice;

    @Value("${consumption.mode.sunny.days.around.summer.solstice}")
    private int sunnyModeNbrDaysAroundSummerSolstice;

    ParameterRepository parameterRepository;

    Logger logger = LoggerFactory.getLogger(ConfigController.class);

    public ConfigController(ParameterRepository parameterRepository) {
        this.parameterRepository = parameterRepository;
    }

    @CacheEvict(value = "lightSecurityTimerDelayEco")
    public void setLightSecurityTimerDelayEco(long lightSecurityTimerDelayEco) {
        Parameter parameter = new Parameter();
        parameter.setKey("light.security.timer.delay.eco");
        parameter.setValue(String.valueOf(lightSecurityTimerDelayEco));
        logger.info("Saving it db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "lightSecurityTimerDelayRegular")
    public void setLightSecurityTimerDelayRegular(long lightSecurityTimerDelayRegular) {
        Parameter parameter = new Parameter();
        parameter.setKey("light.security.timer.delay.eco");
        parameter.setValue(String.valueOf(lightSecurityTimerDelayRegular));
        logger.info("Saving it db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "lightSecurityTimerDelaySunny")
    public void setLightSecurityTimerDelaySunny(long lightSecurityTimerDelaySunny) {
        Parameter parameter = new Parameter();
        parameter.setKey("light.security.timer.delay.eco");
        parameter.setValue(String.valueOf(lightSecurityTimerDelaySunny));
        logger.info("Saving it db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @Cacheable(value = {"lightSecurityTimerDelayEco"})
    public long getLightSecurityTimerDelayEco() {
        Parameter parameter = parameterRepository.findByKey("light.security.timer.delay.eco");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return lightSecurityTimerDelayEco;
    }

    @Cacheable(value = {"lightSecurityTimerDelayRegular"})
    public long getLightSecurityTimerDelayRegular() {
        Parameter parameter = parameterRepository.findByKey("light.security.timer.delay.regular");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return lightSecurityTimerDelayRegular;
    }

    @Cacheable(value = {"lightSecurityTimerDelaySunny"})
    public long getLightSecurityTimerDelaySunny() {
        Parameter parameter = parameterRepository.findByKey("light.security.timer.delay.sunny");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return lightSecurityTimerDelaySunny;
    }

    @CacheEvict(value = "fanSecurityTimerDelayEco")
    public void setFanSecurityTimerDelayEco(long fanSecurityTimerDelayEco) {
        Parameter parameter = new Parameter();
        parameter.setKey("fan.security.timer.delay.eco");
        parameter.setValue(String.valueOf(fanSecurityTimerDelayEco));
        logger.info("Saving it db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "fanSecurityTimerDelayRegular")
    public void setFanSecurityTimerDelayRegular(long fanSecurityTimerDelayRegular) {
        Parameter parameter = new Parameter();
        parameter.setKey("fan.security.timer.delay.eco");
        parameter.setValue(String.valueOf(fanSecurityTimerDelayRegular));
        logger.info("Saving it db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "fanSecurityTimerDelaySunny")
    public void setFanSecurityTimerDelaySunny(long fanSecurityTimerDelaySunny) {
        Parameter parameter = new Parameter();
        parameter.setKey("fan.security.timer.delay.eco");
        parameter.setValue(String.valueOf(fanSecurityTimerDelaySunny));
        logger.info("Saving it db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @Cacheable(value = {"fanSecurityTimerDelayEco"})
    public long getFanSecurityTimerDelayEco() {
        Parameter parameter = parameterRepository.findByKey("fan.security.timer.delay.eco");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return fanSecurityTimerDelayEco;
    }

    @Cacheable(value = {"fanSecurityTimerDelayRegular"})
    public long getFanSecurityTimerDelayRegular() {
        Parameter parameter = parameterRepository.findByKey("fan.security.timer.delay.regular");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return fanSecurityTimerDelayRegular;
    }

    @Cacheable(value = {"fanSecurityTimerDelaySunny"})
    public long getFanSecurityTimerDelaySunny() {
        Parameter parameter = parameterRepository.findByKey("fan.security.timer.delay.sunny");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return fanSecurityTimerDelaySunny;
    }

    @CacheEvict(value = "musicSecurityTimerDelayEco")
    public void setMusicSecurityTimerDelayEco(long musicSecurityTimerDelayEco) {
        Parameter parameter = new Parameter();
        parameter.setKey("music.security.timer.delay.eco");
        parameter.setValue(String.valueOf(musicSecurityTimerDelayEco));
        logger.info("Saving in db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "musicSecurityTimerDelayRegular")
    public void setMusicSecurityTimerDelayRegular(long musicSecurityTimerDelayRegular) {
        Parameter parameter = new Parameter();
        parameter.setKey("music.security.timer.delay.eco");
        parameter.setValue(String.valueOf(musicSecurityTimerDelayRegular));
        logger.info("Saving in db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "musicSecurityTimerDelaySunny")
    public void setMusicSecurityTimerDelaySunny(long musicSecurityTimerDelaySunny) {
        Parameter parameter = new Parameter();
        parameter.setKey("music.security.timer.delay.eco");
        parameter.setValue(String.valueOf(musicSecurityTimerDelaySunny));
        logger.info("Saving in db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @Cacheable(value = {"musicSecurityTimerDelayEco"})
    public long getMusicSecurityTimerDelayEco() {
        Parameter parameter = parameterRepository.findByKey("music.security.timer.delay.eco");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return musicSecurityTimerDelayEco;
    }

    @Cacheable(value = {"musicSecurityTimerDelayRegular"})
    public long getMusicSecurityTimerDelayRegular() {
        Parameter parameter = parameterRepository.findByKey("music.security.timer.delay.regular");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return musicSecurityTimerDelayRegular;
    }

    @Cacheable(value = {"musicSecurityTimerDelaySunny"})
    public long getMusicSecurityTimerDelaySunny() {
        Parameter parameter = parameterRepository.findByKey("music.security.timer.delay.sunny");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Long.valueOf(parameter.getValue());
        }
        return musicSecurityTimerDelaySunny;
    }

    @Cacheable(value = {"ecoModeNbrDaysAroundWinterSolstice"})
    public int getEcoModeNbrDaysAroundWinterSolstice() {
        Parameter parameter = parameterRepository.findByKey("consumption.mode.eco.days.around.winter.solstice");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Integer.valueOf(parameter.getValue());
        }
        return ecoModeNbrDaysAroundWinterSolstice;
    }

    @Cacheable(value = {"sunnyModeNbrDaysAroundSummerSolstice"})
    public int getSunnyModeNbrDaysAroundSummerSolstice() {
        Parameter parameter = parameterRepository.findByKey("consumption.mode.sunny.days.around.summer.solstice");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getValue())) {
            return Integer.valueOf(parameter.getValue());
        }
        return sunnyModeNbrDaysAroundSummerSolstice;
    }

    @CacheEvict(value = "ecoModeNbrDaysAroundWinterSolstice")
    public void setEcoModeNbrDaysAroundWinterSolstice(int ecoModeNbrDaysAroundWinterSolstice) {
        Parameter parameter = new Parameter();
        parameter.setKey("consumption.mode.eco.days.around.winter.solstice");
        parameter.setValue(String.valueOf(ecoModeNbrDaysAroundWinterSolstice));
        logger.info("Saving in db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "sunnyModeNbrDaysAroundSummerSolstice")
    public void setSunnyModeNbrDaysAroundSummerSolstice(int sunnyModeNbrDaysAroundSummerSolstice) {
        Parameter parameter = new Parameter();
        parameter.setKey("consumption.mode.sunny.days.around.summer.solstice");
        parameter.setValue(String.valueOf(sunnyModeNbrDaysAroundSummerSolstice));
        logger.info("Saving in db {}.", parameter.getKey());
        parameterRepository.save(parameter);
    }
}
