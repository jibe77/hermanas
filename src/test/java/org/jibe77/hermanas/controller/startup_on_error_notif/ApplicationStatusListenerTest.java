package org.jibe77.hermanas.controller.startup_on_error_notif;

import org.jibe77.hermanas.client.email.EmailService;
import org.jibe77.hermanas.controller.camera.CameraController;
import org.jibe77.hermanas.data.entity.Event;
import org.jibe77.hermanas.data.entity.EventType;
import org.jibe77.hermanas.data.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.util.Optional;

class ApplicationStatusListenerTest {

    ApplicationStatusListener applicationStatusListener;

    EmailService emailService = Mockito.mock(EmailService.class);

    CameraController cameraController = Mockito.mock(CameraController.class);

    EventRepository eventRepository = Mockito.mock(EventRepository.class);

    @BeforeEach
    public void setUp() {
        applicationStatusListener = new ApplicationStatusListener(eventRepository, emailService, cameraController);
    }

    @Test
    void testInitWithoutShutdownError(){
        Event event = new Event();
        event.setEventType(EventType.SHUTDOWN);
        Mockito.when(eventRepository.findTopByEventTypeInOrderByDateTimeDesc(Mockito.any())).thenReturn(event);

        applicationStatusListener.init();

        Mockito.verify(eventRepository, Mockito.times(1)).save(Mockito.any(Event.class));
        Mockito.verify(emailService,Mockito.times(0)).sendMailWithAttachment(Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class));
        Mockito.verify(emailService,Mockito.times(0)).sendMail(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testInitWithShutdownError(){
        Event event = new Event();
        event.setEventType(EventType.STARTUP);
        Mockito.when(eventRepository.findTopByEventTypeInOrderByDateTimeDesc(Mockito.any())).thenReturn(event);
        Optional<File> o = Optional.of(new File(""));
        Mockito.when(cameraController.takePictureNoException()).thenReturn(o);

        applicationStatusListener.init();

        Mockito.verify(eventRepository, Mockito.times(1)).save(Mockito.any(Event.class));
        Mockito.verify(emailService,Mockito.times(1)).sendMailWithAttachment(Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class));
        Mockito.verify(emailService,Mockito.times(0)).sendMail(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testInitWithShutdownErrorWithoutCameraPicture(){
        Event event = new Event();
        event.setEventType(EventType.STARTUP);
        Mockito.when(eventRepository.findTopByEventTypeInOrderByDateTimeDesc(Mockito.any())).thenReturn(event);
        Optional<File> o = Optional.ofNullable(null);
        Mockito.when(cameraController.takePictureNoException()).thenReturn(o);

        applicationStatusListener.init();

        Mockito.verify(eventRepository, Mockito.times(1)).save(Mockito.any(Event.class));
        Mockito.verify(emailService,Mockito.times(0)).sendMailWithAttachment(Mockito.anyString(), Mockito.anyString(), Mockito.any(File.class));
        Mockito.verify(emailService,Mockito.times(1)).sendMail(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void testDestroy(){
        applicationStatusListener.destroy();

        Mockito.verify(eventRepository, Mockito.times(1)).save(Mockito.any(Event.class));
        Mockito.verify(eventRepository).save(Mockito.argThat((Event event) -> event.getEventType() == EventType.SHUTDOWN));
    }
}
