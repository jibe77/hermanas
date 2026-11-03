package org.jibe77.hermanas.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${cors.origins.allowed}")
    public String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic");
    }

    // Inbound/outbound STOMP channels default to 2 x CPU threads each (8 on the Pi);
    // a few dashboards receiving status pushes need far less.
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(1).maxPoolSize(2);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(1).maxPoolSize(2);
    }

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        StompWebSocketEndpointRegistration registration = registry.addEndpoint("/api/v1/stomp");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            logger.info("setting allowed origin to {}.", allowedOrigins);
            registration.setAllowedOrigins(allowedOrigins);
        } else {
            logger.info("no allowed origin to web-socket.");
        }
    }
}
