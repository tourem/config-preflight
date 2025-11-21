package com.mycompany.validator.micronaut;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.PlaceholderDetector;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

/**
 * Validateur précoce pour Micronaut.
 * S'exécute au tout début du démarrage via ApplicationEventListener<StartupEvent>.
 * 
 * Implémente Ordered avec HIGHEST_PRECEDENCE pour s'exécuter avant les beans d'infrastructure.
 */
@Singleton
public class MicronautEarlyValidator implements ApplicationEventListener<StartupEvent>, Ordered {
    
    private final io.micronaut.context.env.Environment environment;
    private final PlaceholderDetector placeholderDetector = new PlaceholderDetector();
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    public MicronautEarlyValidator(io.micronaut.context.env.Environment environment) {
        this.environment = environment;
    }
    
    @Override
    public void onApplicationEvent(StartupEvent event) {
        // Vérifier si le validateur est activé
        String enabled = environment.getProperty("configuration.validator.enabled", String.class)
                                   .orElse("true");
        if ("false".equalsIgnoreCase(enabled)) {
            return;
        }
        
        // Vérifier si la validation précoce est activée
        String earlyValidation = environment.getProperty("configuration.validator.early-validation", String.class)
                                           .orElse("true");
        if ("false".equalsIgnoreCase(earlyValidation)) {
            return;
        }
        
        // Valider les placeholders
        List<ConfigurationError> errors = validatePlaceholders();
        
        if (!errors.isEmpty()) {
            ValidationResult result = new ValidationResult(errors);
            String formattedErrors = formatter.format(result);
            
            System.err.println(formattedErrors);
            System.err.println("⚠️  Configuration validation failed during early startup phase.");
            System.err.println("💡 Fix the errors above before starting the application.");
            
            // Arrêter l'application immédiatement
            throw new ConfigurationValidationException(
                "Configuration validation failed with " + errors.size() + " error(s)",
                result
            );
        }
    }
    
    private List<ConfigurationError> validatePlaceholders() {
        List<ConfigurationError> errors = new ArrayList<>();
        MicronautPropertyBindingResolver bindingResolver = new MicronautPropertyBindingResolver(environment);
        
        // Parcourir toutes les propriétés
        java.util.Map<String, Object> properties = environment.getProperties(null);
        for (java.util.Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Object valueObj = entry.getValue();
            String value = valueObj instanceof String ? (String) valueObj : null;
            
            if (value != null) {
                // Utiliser detectRequiredPlaceholders pour ignorer ceux avec valeur par défaut
                List<String> requiredPlaceholders = placeholderDetector.detectRequiredPlaceholders(value);
                
                for (String placeholder : requiredPlaceholders) {
                    if (!bindingResolver.propertyExists(placeholder)) {
                        boolean isSensitive = secretDetector.isSensitive(placeholder);
                        
                        errors.add(ConfigurationError.builder()
                            .type(ErrorType.UNRESOLVED_PLACEHOLDER)
                            .propertyName(propertyName)
                            .errorMessage(String.format(
                                "Cannot resolve placeholder '${%s}' in property '%s'",
                                placeholder, propertyName
                            ))
                            .suggestion(bindingResolver.generateSuggestion(placeholder))
                            .isSensitive(isSensitive)
                            .build());
                    }
                }
            }
        }
        
        return errors;
    }
    
    @Override
    public int getOrder() {
        // HIGHEST_PRECEDENCE pour s'exécuter le plus tôt possible
        return Ordered.HIGHEST_PRECEDENCE;
    }
    
    /**
     * Exception levée lors de l'échec de la validation de configuration.
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
