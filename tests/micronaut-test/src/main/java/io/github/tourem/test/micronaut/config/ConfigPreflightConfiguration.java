package io.github.tourem.test.micronaut.config;

import com.mycompany.validator.micronaut.MicronautConfigurationPropertiesValidator;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

/**
 * Configuration pour activer config-preflight dans le projet de test.
 */
@Factory
public class ConfigPreflightConfiguration {
    
    @Singleton
    public MicronautConfigurationPropertiesValidator configurationPropertiesValidator(BeanContext beanContext) {
        return new MicronautConfigurationPropertiesValidator(beanContext);
    }
}
