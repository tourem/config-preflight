package io.github.tourem.test.micronaut.config;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration pour activer config-preflight dans le projet de test.
 * Le MicronautConfigurationPropertiesValidator s'occupe automatiquement de la validation.
 */
@Singleton
public class ConfigPreflightConfiguration implements ApplicationEventListener<ServerStartupEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigPreflightConfiguration.class);
    
    public ConfigPreflightConfiguration() {
        logger.info("✅ Config-preflight is enabled for this application");
    }
    
    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        logger.info("✅ Application started successfully - all configuration properties are valid");
    }
}
