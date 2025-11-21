package com.mycompany.validator.quarkus;

import com.mycompany.validator.core.api.ConfigurationValidator;
import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.model.*;
import com.mycompany.validator.core.detector.PlaceholderDetector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.*;

/**
 * Validateur pour Quarkus.
 * Utilise MicroProfile Config pour accéder aux propriétés.
 */
@ApplicationScoped
public class QuarkusConfigurationValidator implements ConfigurationValidator {
    
    private final Config config;
    private final PlaceholderDetector placeholderDetector;
    private final QuarkusPropertyBindingResolver bindingResolver;
    
    @Inject
    public QuarkusConfigurationValidator(Config config) {
        this.config = config;
        this.placeholderDetector = new PlaceholderDetector();
        this.bindingResolver = new QuarkusPropertyBindingResolver(config);
    }
    
    public QuarkusConfigurationValidator() {
        this(ConfigProvider.getConfig());
    }
    
    @Override
    public ValidationResult validateAll() {
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Valider toutes les propriétés connues
        for (String propertyName : config.getPropertyNames()) {
            Optional<String> value = config.getOptionalValue(propertyName, String.class);
            
            if (value.isPresent()) {
                String stringValue = value.get();
                
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
        
        for (String propertyName : config.getPropertyNames()) {
            Optional<String> value = config.getOptionalValue(propertyName, String.class);
            
            if (value.isPresent()) {
                String stringValue = value.get();
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
