# Hermanas

This application is an automation solution for your chicken coop.
It's main feature is to schedule door closing and opening according to the sunrise and sunset.
It also offers integration of light, music, fan as well as notification.

Hermanas web front-end is hosted on a separate project on https://github.com/jibe77/hermanasclient.
You may try it now on http://www.hermanas.fr.

## Architecture

Running on top of a Raspberry-pi, it is powered on Spring Boot.
All the monitoring and remote facilities are remotely available through Rest services.

## Siri integration

Rest services may be called from any iOS device using Siri :

TODO

## Actuators

https://poulailler57.ddns.net:5780/actuator/info
https://poulailler57.ddns.net:5780/actuator/health
