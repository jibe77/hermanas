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
        parameter.setEntryKey("light.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(lightSecurityTimerDelayEco));
        logger.info("Saving it db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "lightSecurityTimerDelayRegular")
    public void setLightSecurityTimerDelayRegular(long lightSecurityTimerDelayRegular) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("light.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(lightSecurityTimerDelayRegular));
        logger.info("Saving it db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "lightSecurityTimerDelaySunny")
    public void setLightSecurityTimerDelaySunny(long lightSecurityTimerDelaySunny) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("light.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(lightSecurityTimerDelaySunny));
        logger.info("Saving it db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @Cacheable(value = {"lightSecurityTimerDelayEco"})
    public long getLightSecurityTimerDelayEco() {
        Parameter parameter = parameterRepository.findByEntryKey("light.security.timer.delay.eco");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return lightSecurityTimerDelayEco;
    }

    @Cacheable(value = {"lightSecurityTimerDelayRegular"})
    public long getLightSecurityTimerDelayRegular() {
        Parameter parameter = parameterRepository.findByEntryKey("light.security.timer.delay.regular");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return lightSecurityTimerDelayRegular;
    }

    @Cacheable(value = {"lightSecurityTimerDelaySunny"})
    public long getLightSecurityTimerDelaySunny() {
        Parameter parameter = parameterRepository.findByEntryKey("light.security.timer.delay.sunny");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return lightSecurityTimerDelaySunny;
    }

    @CacheEvict(value = "fanSecurityTimerDelayEco")
    public void setFanSecurityTimerDelayEco(long fanSecurityTimerDelayEco) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("fan.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(fanSecurityTimerDelayEco));
        logger.info("Saving it db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "fanSecurityTimerDelayRegular")
    public void setFanSecurityTimerDelayRegular(long fanSecurityTimerDelayRegular) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("fan.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(fanSecurityTimerDelayRegular));
        logger.info("Saving it db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "fanSecurityTimerDelaySunny")
    public void setFanSecurityTimerDelaySunny(long fanSecurityTimerDelaySunny) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("fan.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(fanSecurityTimerDelaySunny));
        logger.info("Saving it db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @Cacheable(value = {"fanSecurityTimerDelayEco"})
    public long getFanSecurityTimerDelayEco() {
        Parameter parameter = parameterRepository.findByEntryKey("fan.security.timer.delay.eco");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return fanSecurityTimerDelayEco;
    }

    @Cacheable(value = {"fanSecurityTimerDelayRegular"})
    public long getFanSecurityTimerDelayRegular() {
        Parameter parameter = parameterRepository.findByEntryKey("fan.security.timer.delay.regular");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return fanSecurityTimerDelayRegular;
    }

    @Cacheable(value = {"fanSecurityTimerDelaySunny"})
    public long getFanSecurityTimerDelaySunny() {
        Parameter parameter = parameterRepository.findByEntryKey("fan.security.timer.delay.sunny");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return fanSecurityTimerDelaySunny;
    }

    @CacheEvict(value = "musicSecurityTimerDelayEco")
    public void setMusicSecurityTimerDelayEco(long musicSecurityTimerDelayEco) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("music.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(musicSecurityTimerDelayEco));
        logger.info("Saving in db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "musicSecurityTimerDelayRegular")
    public void setMusicSecurityTimerDelayRegular(long musicSecurityTimerDelayRegular) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("music.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(musicSecurityTimerDelayRegular));
        logger.info("Saving in db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "musicSecurityTimerDelaySunny")
    public void setMusicSecurityTimerDelaySunny(long musicSecurityTimerDelaySunny) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("music.security.timer.delay.eco");
        parameter.setEntryValue(String.valueOf(musicSecurityTimerDelaySunny));
        logger.info("Saving in db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @Cacheable(value = {"musicSecurityTimerDelayEco"})
    public long getMusicSecurityTimerDelayEco() {
        Parameter parameter = parameterRepository.findByEntryKey("music.security.timer.delay.eco");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return musicSecurityTimerDelayEco;
    }

    @Cacheable(value = {"musicSecurityTimerDelayRegular"})
    public long getMusicSecurityTimerDelayRegular() {
        Parameter parameter = parameterRepository.findByEntryKey("music.security.timer.delay.regular");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return musicSecurityTimerDelayRegular;
    }

    @Cacheable(value = {"musicSecurityTimerDelaySunny"})
    public long getMusicSecurityTimerDelaySunny() {
        Parameter parameter = parameterRepository.findByEntryKey("music.security.timer.delay.sunny");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Long.valueOf(parameter.getEntryValue());
        }
        return musicSecurityTimerDelaySunny;
    }

    @Cacheable(value = {"ecoModeNbrDaysAroundWinterSolstice"})
    public int getEcoModeNbrDaysAroundWinterSolstice() {
        Parameter parameter = parameterRepository.findByEntryKey("consumption.mode.eco.days.around.winter.solstice");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Integer.valueOf(parameter.getEntryValue());
        }
        return ecoModeNbrDaysAroundWinterSolstice;
    }

    @Cacheable(value = {"sunnyModeNbrDaysAroundSummerSolstice"})
    public int getSunnyModeNbrDaysAroundSummerSolstice() {
        Parameter parameter = parameterRepository.findByEntryKey("consumption.mode.sunny.days.around.summer.solstice");
        if (parameter != null && StringUtils.isNotEmpty(parameter.getEntryValue())) {
            return Integer.valueOf(parameter.getEntryValue());
        }
        return sunnyModeNbrDaysAroundSummerSolstice;
    }

    @CacheEvict(value = "ecoModeNbrDaysAroundWinterSolstice")
    public void setEcoModeNbrDaysAroundWinterSolstice(int ecoModeNbrDaysAroundWinterSolstice) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("consumption.mode.eco.days.around.winter.solstice");
        parameter.setEntryValue(String.valueOf(ecoModeNbrDaysAroundWinterSolstice));
        logger.info("Saving in db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }

    @CacheEvict(value = "sunnyModeNbrDaysAroundSummerSolstice")
    public void setSunnyModeNbrDaysAroundSummerSolstice(int sunnyModeNbrDaysAroundSummerSolstice) {
        Parameter parameter = new Parameter();
        parameter.setEntryKey("consumption.mode.sunny.days.around.summer.solstice");
        parameter.setEntryValue(String.valueOf(sunnyModeNbrDaysAroundSummerSolstice));
        logger.info("Saving in db {}.", parameter.getEntryKey());
        parameterRepository.save(parameter);
    }
}
