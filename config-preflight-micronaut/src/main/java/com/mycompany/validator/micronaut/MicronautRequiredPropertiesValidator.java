package com.mycompany.validator.micronaut;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import com.mycompany.validator.core.model.PropertySource;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Validator pour les propriétés requises de Micronaut.
 * Lit la liste des propriétés requises depuis META-INF/config-preflight.properties
 * et vérifie qu'elles sont toutes définies.
 */
@Singleton
@Requires(property = "configuration.validator.enabled", value = "true", defaultValue = "true")
public class MicronautRequiredPropertiesValidator implements ApplicationEventListener<ServerStartupEvent> {
    
    private static final Logger logger = LoggerFactory.getLogger(MicronautRequiredPropertiesValidator.class);
    
    private final Environment environment;
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    public MicronautRequiredPropertiesValidator(Environment environment) {
        this.environment = environment;
    }
    
    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        logger.info("🔍 Validating required configuration properties...");
        
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Charger la liste des propriétés requises depuis config-preflight.properties
        List<String> requiredProperties = loadRequiredProperties();
        
        if (requiredProperties.isEmpty()) {
            logger.debug("No required properties defined in META-INF/config-preflight.properties");
            return;
        }
        
        logger.info("Found {} required properties to validate", requiredProperties.size());
        
        // Vérifier chaque propriété requise dans l'Environment
        for (String propertyName : requiredProperties) {
            String value = environment.getProperty(propertyName, String.class).orElse(null);
            
            if (value == null) {
                boolean isSensitive = secretDetector.isSensitive(propertyName);
                
                logger.warn("Property '{}' is not set", propertyName);
                
                errors.add(ConfigurationError.builder()
                    .type(ErrorType.MISSING_PROPERTY)
                    .propertyName(propertyName)
                    .source(new PropertySource("application.yml", "classpath:application.yml", PropertySource.SourceType.APPLICATION_YAML))
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
        return String.format("Add to application.yml: %s: <value>\nOR set environment variable: export %s=<value>",
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
