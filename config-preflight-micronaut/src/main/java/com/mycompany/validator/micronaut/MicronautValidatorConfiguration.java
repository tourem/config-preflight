package com.mycompany.validator.micronaut;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration pour le validateur Micronaut.
 * S'active automatiquement au démarrage de l'application.
 */
@Singleton
@Requires(property = "configuration.validator.enabled", value = "true", defaultValue = "true")
public class MicronautValidatorConfiguration implements ApplicationEventListener<ServerStartupEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(MicronautValidatorConfiguration.class);
    
    private final MicronautConfigurationValidator validator;
    private final BeautifulErrorFormatter formatter;
    
    public MicronautValidatorConfiguration(Environment environment) {
        this.validator = new MicronautConfigurationValidator(environment);
        this.formatter = new BeautifulErrorFormatter();
    }
    
    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        logger.info("Running configuration validation...");
        
        // Valider les placeholders
        ValidationResult result = validator.validatePlaceholders();
        
        if (result.hasErrors()) {
            String formattedErrors = formatter.format(result);
            logger.error(formattedErrors);
            
            // Arrêter l'application si des erreurs sont trouvées
            throw new ConfigurationValidationException(
                "Configuration validation failed with " + result.getErrorCount() + " error(s)",
                result
            );
        } else {
            logger.info("✅ Configuration validation passed!");
        }
    }
    
    /**
     * Exception levée quand la validation échoue.
     */
    public static class ConfigurationValidationException extends RuntimeException {
        private final ValidationResult validationResult;
        
        public ConfigurationValidationException(String message, ValidationResult validationResult) {
            super(message);
            this.validationResult = validationResult;
        }
        
        public ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
