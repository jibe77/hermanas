package org.jibe77.hermanas.service.startup_on_error_notif;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.service.camera.CameraService;
import org.jibe77.hermanas.service.energy.WifiService;
import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.jibe77.hermanas.scheduler.sun.ConsumptionModeController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.event.ContextClosedEvent;

import java.io.File;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ApplicationStatusListenerTest {

    ApplicationStatusListener applicationStatusListener;

    EmailService emailService = mock(EmailService.class);

    CameraService cameraService = mock(CameraService.class);

    EventRepository eventRepository = mock(EventRepository.class);

    MessageSource messageSource = mock(MessageSource.class);

    WifiService wifiService = mock(WifiService.class);

    ConsumptionModeController consumptionModeController = mock(ConsumptionModeController.class);

    @BeforeEach
    public void setUp() {
        applicationStatusListener = new ApplicationStatusListener(eventRepository, emailService, cameraService,
                messageSource, wifiService, consumptionModeController);
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
        Mockito.when(cameraService.takePictureNoException(true)).thenReturn(o);

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
        Mockito.when(cameraService.takePictureNoException(false)).thenReturn(o);

        applicationStatusListener.init();

        verify(eventRepository, Mockito.times(1)).save(any(Event.class));
        verify(emailService,Mockito.times(1)).sendMail(Mockito.anyString(), Mockito.anyString(), Mockito.any(Optional.class));
    }

    @Test
    void testOnContextClosed(){
        applicationStatusListener.onApplicationEvent(mock(ContextClosedEvent.class));

        verify(eventRepository, Mockito.times(1)).save(any(Event.class));
        verify(eventRepository).save(Mockito.argThat((Event event) -> event.getEventType() == EventType.SHUTDOWN));
    }
}
