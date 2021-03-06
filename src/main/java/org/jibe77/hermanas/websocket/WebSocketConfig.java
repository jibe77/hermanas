package org.jibe77.hermanas.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${cors.origins.allowed}")
    public String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/socket");
    }

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        StompWebSocketEndpointRegistration registration = registry.addEndpoint("/socket");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            logger.info("setting allowed origin to {}.", allowedOrigins);
            registration = registration.setAllowedOriginPatterns(allowedOrigins);
        } else {
            logger.info("no allowed origin to web-socket.");
        }
        registration.withSockJS();
    }
}
