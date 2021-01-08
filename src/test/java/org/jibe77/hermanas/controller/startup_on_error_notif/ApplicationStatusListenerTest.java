package org.jibe77.hermanas.controller.startup_on_error_notif;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.controller.energy.WifiController;
import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;

import java.io.File;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApplicationStatusListenerTest {

    ApplicationStatusListener applicationStatusListener;

    EmailService emailService = mock(EmailService.class);

    CameraController cameraController = mock(CameraController.class);

    EventRepository eventRepository = mock(EventRepository.class);

    MessageSource messageSource = mock(MessageSource.class);

    WifiController wifiController = mock(WifiController.class);

    ConsumptionModeManager consumptionModeManager = mock(ConsumptionModeManager.class);

    @BeforeEach
    public void setUp() {
        applicationStatusListener = new ApplicationStatusListener(eventRepository, emailService, cameraController,
                messageSource, wifiController, consumptionModeManager);
        when(messageSource.getMessage(anyString(), any(), any())).thenReturn("test");
    }

    @Test
    void testInitWithoutShutdownError(){
        Event event = new Event();
        event.setEventType(EventType.SHUTDOWN);
        Mockito.when(eventRepository.findTopByEventTypeInOrderByDateTimeDesc(any())).thenReturn(event);

        applicationStatusListener.init();

        verify(eventRepository, Mockito.times(1)).save(any(Event.class));
        verify(emailService,Mockito.times(0)).sendMail(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testInitWithShutdownError(){
        Event event = new Event();
        event.setEventType(EventType.STARTUP);
        Mockito.when(eventRepository.findTopByEventTypeInOrderByDateTimeDesc(any())).thenReturn(event);
        Optional<File> o = Optional.of(new File(""));
        Mockito.when(cameraController.takePictureNoException(true)).thenReturn(o);

        applicationStatusListener.init();

        verify(eventRepository, Mockito.times(1)).save(any(Event.class));
        verify(emailService,Mockito.times(1)).sendMail(Mockito.anyString(), Mockito.anyString(), any(Optional.class));
    }

    @Test
    void testInitWithShutdownErrorWithoutCameraPicture(){
        Event event = new Event();
        event.setEventType(EventType.STARTUP);
        Mockito.when(eventRepository.findTopByEventTypeInOrderByDateTimeDesc(any())).thenReturn(event);
        Optional<File> o = Optional.ofNullable(null);
        Mockito.when(cameraController.takePictureNoException(false)).thenReturn(o);

        applicationStatusListener.init();

        verify(eventRepository, Mockito.times(1)).save(any(Event.class));
        verify(emailService,Mockito.times(1)).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.any(Optional.class));
    }

    @Test
    void testDestroy(){
        applicationStatusListener.destroy();

        verify(eventRepository, Mockito.times(1)).save(any(Event.class));
        verify(eventRepository).save(Mockito.argThat((Event event) -> event.getEventType() == EventType.SHUTDOWN));
    }
}
