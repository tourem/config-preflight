package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ConfigurationValidator;
import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.model.*;
import com.mycompany.validator.core.detector.PlaceholderDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validateur pour Spring Boot.
 * RÉUTILISE les PropertySources déjà chargées par Spring Boot.
 * 
 * v2 : Support complet du binding app.database.url <-> APP_DATABASE_URL
 */
@Component
public class SpringBootConfigurationValidator implements ConfigurationValidator {
    
    private final Environment environment;
    private final PlaceholderDetector placeholderDetector;
    private final SpringBootPropertyBindingResolver bindingResolver;
    private final SpringBootBinderPropertyResolver binderResolver;
    
    @Autowired
    public SpringBootConfigurationValidator(Environment environment) {
        this.environment = environment;
        this.placeholderDetector = new PlaceholderDetector();
        this.bindingResolver = new SpringBootPropertyBindingResolver(environment);
        this.binderResolver = new SpringBootBinderPropertyResolver(environment);
    }
    
    @Override
    public ValidationResult validateAll() {
        List<ConfigurationError> errors = new ArrayList<>();
        
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
            MutablePropertySources propertySources = configurableEnv.getPropertySources();
            
            for (org.springframework.core.env.PropertySource<?> propertySource : propertySources) {
                errors.addAll(validatePropertySource(propertySource));
            }
        }
        
        return new ValidationResult(errors);
    }
    
    @Override
    public ValidationResult validateRequired(String... requiredProperties) {
        List<ConfigurationError> errors = new ArrayList<>();
        
        for (String property : requiredProperties) {
            // ✅ Utiliser le Binder API pour une résolution 100% fidèle à Spring Boot
            if (!binderResolver.propertyExists(property)) {
                errors.add(ConfigurationError.builder()
                    .type(ErrorType.MISSING_PROPERTY)
                    .propertyName(property)
                    .errorMessage(String.format(
                        "Property '%s' is required but not defined (Spring Boot Binder could not resolve it)",
                        property
                    ))
                    .suggestion(bindingResolver.generateSuggestion(property))
                    .source(findPropertySource(property))
                    .build());
            } else {
                // La propriété existe, vérifier si elle est vide
                String value = binderResolver.getPropertyValue(property);
                if (value != null && value.trim().isEmpty()) {
                    errors.add(ConfigurationError.builder()
                        .type(ErrorType.EMPTY_VALUE)
                        .propertyName(property)
                        .errorMessage(String.format(
                            "Property '%s' is defined but has an empty value",
                            property
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
        
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
            MutablePropertySources propertySources = configurableEnv.getPropertySources();
            
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
                                // ✅ Utiliser le Binder API pour vérifier l'existence
                                if (!binderResolver.propertyExists(placeholder)) {
                                    errors.add(ConfigurationError.builder()
                                        .type(ErrorType.UNRESOLVED_PLACEHOLDER)
                                        .propertyName(propertyName)
                                        .errorMessage(String.format(
                                            "Cannot resolve placeholder '${%s}' in property '%s' " +
                                            "(Spring Boot Binder could not find this property)",
                                            placeholder, propertyName
                                        ))
                                        .suggestion(bindingResolver.generateSuggestion(placeholder))
                                        .source(toPropertySource(propertySource))
                                        .build());
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return new ValidationResult(errors);
    }
    
    private List<ConfigurationError> validatePropertySource(
            org.springframework.core.env.PropertySource<?> propertySource) {
        
        List<ConfigurationError> errors = new ArrayList<>();
        
        String name = propertySource.getName();
        if (name.contains("Config resource 'file") && name.contains("not found")) {
            errors.add(ConfigurationError.builder()
                .type(ErrorType.IMPORT_FILE_INACCESSIBLE)
                .propertyName("spring.config.import")
                .errorMessage(name)
                .suggestion("Verify the file path or URL in spring.config.import")
                .build());
        }
        
        return errors;
    }
    
    private PropertySource findPropertySource(String propertyName) {
        if (environment instanceof ConfigurableEnvironment) {
            ConfigurableEnvironment configurableEnv = (ConfigurableEnvironment) environment;
            MutablePropertySources propertySources = configurableEnv.getPropertySources();
            
            // Chercher sous toutes les variantes
            String actualName = bindingResolver.findActualPropertyName(propertyName);
            if (actualName != null) {
                for (org.springframework.core.env.PropertySource<?> ps : propertySources) {
                    if (ps.containsProperty(actualName)) {
                        return toPropertySource(ps);
                    }
                }
            }
        }
        
        return null;
    }
    
    private PropertySource toPropertySource(org.springframework.core.env.PropertySource<?> springSource) {
        String name = springSource.getName();
        PropertySource.SourceType type = determineSourceType(name);
        
        return new PropertySource(name, extractLocation(name), type);
    }
    
    private PropertySource.SourceType determineSourceType(String sourceName) {
        if (sourceName.contains("applicationConfig")) {
            if (sourceName.contains(".yml") || sourceName.contains(".yaml")) {
                return PropertySource.SourceType.APPLICATION_YAML;
            }
            return PropertySource.SourceType.APPLICATION_PROPERTIES;
        }
        if (sourceName.contains("systemProperties")) {
            return PropertySource.SourceType.SYSTEM_PROPERTY;
        }
        if (sourceName.contains("systemEnvironment")) {
            return PropertySource.SourceType.ENVIRONMENT_VARIABLE;
        }
        if (sourceName.contains("commandLineArgs")) {
            return PropertySource.SourceType.COMMAND_LINE;
        }
        if (sourceName.contains("Config resource")) {
            return PropertySource.SourceType.IMPORTED_FILE;
        }
        
        return PropertySource.SourceType.UNKNOWN;
    }
    
    private String extractLocation(String sourceName) {
        if (sourceName.contains("'")) {
            int start = sourceName.indexOf("'") + 1;
            int end = sourceName.lastIndexOf("'");
            if (start > 0 && end > start) {
                return sourceName.substring(start, end);
            }
        }
        return sourceName;
    }
}
