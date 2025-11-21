package com.mycompany.validator.micronaut;

import com.mycompany.validator.core.api.ConfigurationValidator;
import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.model.*;
import com.mycompany.validator.core.detector.PlaceholderDetector;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;

import java.util.*;

/**
 * Validateur pour Micronaut.
 * Utilise l'Environment de Micronaut pour accéder aux propriétés.
 */
@Singleton
public class MicronautConfigurationValidator implements ConfigurationValidator {
    
    private final Environment environment;
    private final PlaceholderDetector placeholderDetector;
    private final MicronautPropertyBindingResolver bindingResolver;
    
    public MicronautConfigurationValidator(Environment environment) {
        this.environment = environment;
        this.placeholderDetector = new PlaceholderDetector();
        this.bindingResolver = new MicronautPropertyBindingResolver(environment);
    }
    
    @Override
    public ValidationResult validateAll() {
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Valider toutes les propriétés connues
        Map<String, Object> properties = environment.getProperties(null);
        
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String stringValue = (String) value;
                
                // Vérifier les placeholders
                List<String> placeholders = placeholderDetector.detectPlaceholders(stringValue);
                for (String placeholder : placeholders) {
                    if (!bindingResolver.propertyExists(placeholder)) {
                        errors.add(ConfigurationError.builder()
                            .type(ErrorType.UNRESOLVED_PLACEHOLDER)
                            .propertyName(propertyName)
                            .errorMessage(String.format(
                                "Cannot resolve placeholder '${%s}' in property '%s'",
                                placeholder, propertyName
                            ))
                            .suggestion(bindingResolver.generateSuggestion(placeholder))
                            .build());
                    }
                }
            }
        }
        
        return new ValidationResult(errors);
    }
    
    @Override
    public ValidationResult validateRequired(String... requiredProperties) {
        List<ConfigurationError> errors = new ArrayList<>();
        
        for (String property : requiredProperties) {
            if (!bindingResolver.propertyExists(property)) {
                errors.add(ConfigurationError.builder()
                    .type(ErrorType.MISSING_PROPERTY)
                    .propertyName(property)
                    .errorMessage(String.format(
                        "Property '%s' is required but not defined (checked all variants: %s, %s, etc.)",
                        property,
                        property,
                        bindingResolver.getBaseResolver().toEnvironmentVariableName(property)
                    ))
                    .suggestion(bindingResolver.generateSuggestion(property))
                    .build());
            } else {
                // La propriété existe, vérifier si elle est vide
                String value = bindingResolver.getPropertyValue(property);
                if (value != null && value.trim().isEmpty()) {
                    String actualName = bindingResolver.findActualPropertyName(property);
                    errors.add(ConfigurationError.builder()
                        .type(ErrorType.EMPTY_VALUE)
                        .propertyName(property)
                        .errorMessage(String.format(
                            "Property '%s' is defined as '%s' but has an empty value",
                            property, actualName
                        ))
                        .suggestion("Set a non-empty value for " + property)
                        .build());
                }
            }
        }
        
        return new ValidationResult(errors);
    }
    
    @Override
    public ValidationResult validatePlaceholders() {
        List<ConfigurationError> errors = new ArrayList<>();
        
        Map<String, Object> properties = environment.getProperties(null);
        
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String stringValue = (String) value;
                // Utiliser detectRequiredPlaceholders pour ignorer ceux avec valeur par défaut
                List<String> requiredPlaceholders = placeholderDetector.detectRequiredPlaceholders(stringValue);
                
                for (String placeholder : requiredPlaceholders) {
                    if (!bindingResolver.propertyExists(placeholder)) {
                        String envVarName = bindingResolver.getBaseResolver().toEnvironmentVariableName(placeholder);
                        
                        errors.add(ConfigurationError.builder()
                            .type(ErrorType.UNRESOLVED_PLACEHOLDER)
                            .propertyName(propertyName)
                            .errorMessage(String.format(
                                "Cannot resolve placeholder '${%s}' in property '%s' " +
                                "(tried: %s, %s, and other variants)",
                                placeholder, propertyName, placeholder, envVarName
                            ))
                            .suggestion(bindingResolver.generateSuggestion(placeholder))
                            .build());
                    }
                }
            }
        }
        
        return new ValidationResult(errors);
    }
}
