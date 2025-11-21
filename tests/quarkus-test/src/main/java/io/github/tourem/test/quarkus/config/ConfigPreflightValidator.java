package io.github.tourem.test.quarkus.config;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import com.mycompany.validator.core.model.PropertySource;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Validator pour les propriétés de configuration Quarkus.
 * Vérifie que toutes les propriétés requises sont définies.
 */
@ApplicationScoped
public class ConfigPreflightValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigPreflightValidator.class);
    
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    // Liste des propriétés requises
    private static final List<String> REQUIRED_PROPERTIES = Arrays.asList(
        "database.url",
        "database.username",
        "database.password",
        "database.max-connections",
        "database.timeout",
        "api.endpoint",
        "api.api-key",
        "api.retry-count",
        "api.enable-cache",
        "api.cache-directory",
        "messaging.broker-url",
        "messaging.queue-name",
        "messaging.username",
        "messaging.password",
        "messaging.connection-timeout",
        "messaging.auto-reconnect"
    );
    
    /**
     * Méthode appelée au démarrage de Quarkus.
     * Utilise une priorité plus basse que QuarkusEarlyValidator pour s'exécuter après.
     */
    public void onStart(@Observes @Priority(100) StartupEvent event) {
        logger.info("🔍 Scanning required configuration properties...");
        
        Config config = ConfigProvider.getConfig();
        
        // Debug: vérifier quel profil est actif
        Optional<String> profile = config.getOptionalValue("quarkus.profile", String.class);
        logger.info("Active profile: {}", profile.orElse("prod"));
        
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Vérifier chaque propriété requise
        for (String propertyName : REQUIRED_PROPERTIES) {
            Optional<String> value = config.getOptionalValue(propertyName, String.class);
            logger.debug("Checking property '{}': {}", propertyName, value.isPresent() ? "SET" : "NOT SET");
            
            if (!value.isPresent()) {
                boolean isSensitive = secretDetector.isSensitive(propertyName);
                
                logger.warn("Property '{}' is not set", propertyName);
                
                errors.add(ConfigurationError.builder()
                    .type(ErrorType.MISSING_PROPERTY)
                    .propertyName(propertyName)
                    .source(new PropertySource("application.properties", "classpath:application.properties", PropertySource.SourceType.APPLICATION_PROPERTIES))
                    .errorMessage("Property '" + propertyName + "' is not set")
                    .suggestion(generateSuggestion(propertyName))
                    .isSensitive(isSensitive)
                    .build());
            }
        }
        
        if (!errors.isEmpty()) {
            ValidationResult result = new ValidationResult(errors);
            String formattedErrors = formatter.format(result);
            
            System.err.println(formattedErrors);
            logger.error("❌ Configuration validation failed with {} error(s)", errors.size());
            
            // Arrêter l'application
            throw new ConfigurationValidationException(
                "Configuration validation failed with " + errors.size() + " error(s)",
                result
            );
        } else {
            logger.info("✅ All required configuration properties are set");
        }
    }
    
    private String generateSuggestion(String propertyName) {
        String envVarName = propertyName.replace('.', '_').replace('-', '_').toUpperCase();
        return String.format("Add to application.properties: %s=<value>\nOR set environment variable: export %s=<value>",
                propertyName, envVarName);
    }
    
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
