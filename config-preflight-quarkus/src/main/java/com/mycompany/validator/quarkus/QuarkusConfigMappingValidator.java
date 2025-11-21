package com.mycompany.validator.quarkus;

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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * Validator pour les interfaces @ConfigMapping de Quarkus.
 * Vérifie que toutes les propriétés requises sont définies.
 */
@ApplicationScoped
public class QuarkusConfigMappingValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(QuarkusConfigMappingValidator.class);
    
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    /**
     * Méthode appelée au démarrage de Quarkus.
     * Utilise une priorité plus basse que QuarkusEarlyValidator pour s'exécuter après.
     */
    public void onStart(@Observes @Priority(100) StartupEvent event) {
        Config config = ConfigProvider.getConfig();
        
        // Vérifier si le validateur est activé
        String enabled = config.getOptionalValue("configuration.validator.enabled", String.class)
                               .orElse("true");
        if ("false".equalsIgnoreCase(enabled)) {
            return;
        }
        
        logger.info("🔍 Validating required configuration properties...");
        
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Charger la liste des propriétés requises depuis config-preflight.properties
        List<String> requiredProperties = loadRequiredProperties();
        
        if (requiredProperties.isEmpty()) {
            logger.debug("No required properties defined in META-INF/config-preflight.properties");
            return;
        }
        
        logger.info("Found {} required properties to validate", requiredProperties.size());
        
        // Vérifier chaque propriété requise
        for (String propertyName : requiredProperties) {
            Optional<String> value = config.getOptionalValue(propertyName, String.class);
            
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
            logger.info("✅ All @ConfigMapping properties are properly configured");
        }
    }
    
    /**
     * Charge la liste des propriétés requises depuis META-INF/config-preflight.properties
     */
    private List<String> loadRequiredProperties() {
        List<String> properties = new ArrayList<>();
        
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("META-INF/config-preflight.properties")) {
            
            if (is == null) {
                logger.debug("META-INF/config-preflight.properties not found");
                return properties;
            }
            
            Properties props = new Properties();
            props.load(is);
            
            // Extraire les noms de propriétés depuis les clés "required.properties.xxx"
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("required.properties.")) {
                    String propertyName = key.substring("required.properties.".length());
                    properties.add(propertyName);
                }
            }
            
        } catch (IOException e) {
            logger.warn("Failed to load config-preflight.properties: {}", e.getMessage());
        }
        
        return properties;
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
