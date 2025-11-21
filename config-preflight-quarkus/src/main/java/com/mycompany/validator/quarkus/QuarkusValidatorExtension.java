package com.mycompany.validator.quarkus;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Extension Quarkus pour valider la configuration au démarrage.
 */
@ApplicationScoped
public class QuarkusValidatorExtension {
    
    private static final Logger logger = Logger.getLogger(QuarkusValidatorExtension.class);
    
    @Inject
    QuarkusConfigurationValidator validator;
    
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    void onStart(@Observes StartupEvent event) {
        Config config = ConfigProvider.getConfig();
        
        // Vérifier si le validateur est activé
        boolean enabled = config.getOptionalValue("configuration.validator.enabled", Boolean.class)
                .orElse(true);
        
        if (!enabled) {
            logger.info("Configuration validator is disabled");
            return;
        }
        
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
