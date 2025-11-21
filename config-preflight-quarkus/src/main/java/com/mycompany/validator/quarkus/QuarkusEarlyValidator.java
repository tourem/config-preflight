package com.mycompany.validator.quarkus;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.PlaceholderDetector;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Validateur précoce pour Quarkus.
 * S'exécute au tout début du démarrage via @Observer(StartupEvent) avec priorité haute.
 * 
 * Utilise Priority.PLATFORM_BEFORE (= 25) pour s'exécuter avant les beans d'infrastructure.
 */
@ApplicationScoped
public class QuarkusEarlyValidator {
    
    private final PlaceholderDetector placeholderDetector = new PlaceholderDetector();
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    /**
     * Méthode appelée au démarrage de Quarkus, AVANT l'initialisation des beans.
     * La priorité PLATFORM_BEFORE (25) garantit l'exécution très tôt.
     * 
     * @param event Événement de démarrage
     */
    public void onStart(@Observes @Priority(25) StartupEvent event) {
        Config config = ConfigProvider.getConfig();
        
        // Vérifier si le validateur est activé
        String enabled = config.getOptionalValue("configuration.validator.enabled", String.class)
                               .orElse("true");
        if ("false".equalsIgnoreCase(enabled)) {
            return;
        }
        
        // Vérifier si la validation précoce est activée
        String earlyValidation = config.getOptionalValue("configuration.validator.early-validation", String.class)
                                       .orElse("true");
        if ("false".equalsIgnoreCase(earlyValidation)) {
            return;
        }
        
        // Valider les placeholders
        List<ConfigurationError> errors = validatePlaceholders(config);
        
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
    
    private List<ConfigurationError> validatePlaceholders(Config config) {
        List<ConfigurationError> errors = new ArrayList<>();
        QuarkusPropertyBindingResolver bindingResolver = new QuarkusPropertyBindingResolver(config);
        
        for (String propertyName : config.getPropertyNames()) {
            String value = config.getOptionalValue(propertyName, String.class).orElse(null);
            
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
