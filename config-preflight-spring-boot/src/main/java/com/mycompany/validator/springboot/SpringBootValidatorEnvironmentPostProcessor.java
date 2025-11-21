package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.PlaceholderDetector;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.ArrayList;
import java.util.List;

/**
 * EnvironmentPostProcessor pour valider la configuration TRÈS TÔT dans le cycle de vie Spring Boot.
 * S'exécute AVANT l'initialisation des beans, y compris JPA, Flyway, etc.
 * 
 * Cela permet de détecter les erreurs de configuration avant que les beans d'infrastructure
 * ne tentent de démarrer avec des propriétés manquantes.
 */
public class SpringBootValidatorEnvironmentPostProcessor implements EnvironmentPostProcessor {
    
    private final PlaceholderDetector placeholderDetector = new PlaceholderDetector();
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Vérifier si le validateur est activé
        String enabled = environment.getProperty("configuration.validator.enabled", "true");
        if ("false".equalsIgnoreCase(enabled)) {
            return;
        }
        
        // Vérifier si la validation précoce est activée
        String earlyValidation = environment.getProperty("configuration.validator.early-validation", "true");
        if ("false".equalsIgnoreCase(earlyValidation)) {
            return;
        }
        
        // Valider les placeholders
        List<ConfigurationError> errors = validatePlaceholders(environment);
        
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
    
    private List<ConfigurationError> validatePlaceholders(ConfigurableEnvironment environment) {
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Utiliser l'API Binder pour une résolution 100% fidèle à Spring Boot
        SpringBootBinderPropertyResolver binderResolver = new SpringBootBinderPropertyResolver(environment);
        
        // Garder aussi l'ancien resolver pour la génération de suggestions
        SpringBootPropertyBindingResolver bindingResolver = new SpringBootPropertyBindingResolver(environment);
        
        MutablePropertySources propertySources = environment.getPropertySources();
        
        for (org.springframework.core.env.PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) propertySource;
                
                for (String propertyName : enumerable.getPropertyNames()) {
                    Object value = propertySource.getProperty(propertyName);
                    
                    if (value instanceof String) {
                        String stringValue = (String) value;
                        
                        // Utiliser detectRequiredPlaceholders pour ignorer ceux avec valeur par défaut
                        List<String> requiredPlaceholders = placeholderDetector.detectRequiredPlaceholders(stringValue);
                        
                        for (String placeholder : requiredPlaceholders) {
                            // Utiliser le Binder API pour vérifier l'existence
                            if (!binderResolver.propertyExists(placeholder)) {
                                String envVarName = bindingResolver.getBaseResolver().toEnvironmentVariableName(placeholder);
                                boolean isSensitive = secretDetector.isSensitive(placeholder);
                                
                                errors.add(ConfigurationError.builder()
                                    .type(ErrorType.UNRESOLVED_PLACEHOLDER)
                                    .propertyName(propertyName)
                                    .errorMessage(String.format(
                                        "Cannot resolve placeholder '${%s}' in property '%s' " +
                                        "(Spring Boot Binder could not find this property)",
                                        placeholder, propertyName
                                    ))
                                    .suggestion(bindingResolver.generateSuggestion(placeholder))
                                    .isSensitive(isSensitive)
                                    .build());
                            }
                        }
                    }
                }
            }
        }
        
        return errors;
    }
    
    /**
     * Exception levée quand la validation échoue pendant le post-processing de l'environnement.
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
