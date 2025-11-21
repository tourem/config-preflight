package com.mycompany.validator.micronaut;

import com.mycompany.validator.core.detector.PropertyBindingResolver;
import io.micronaut.context.env.Environment;

import java.util.List;
import java.util.Optional;

/**
 * Résout les bindings de propriétés pour Micronaut.
 */
public class MicronautPropertyBindingResolver {
    
    private final Environment environment;
    private final PropertyBindingResolver baseResolver;
    
    public MicronautPropertyBindingResolver(Environment environment) {
        this.environment = environment;
        this.baseResolver = new PropertyBindingResolver();
    }
    
    public boolean propertyExists(String propertyName) {
        // Test 1 : Nom original
        Optional<String> value = environment.getProperty(propertyName, String.class);
        if (value.isPresent()) {
            return true;
        }
        
        // Test 2 : Variable d'environnement
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        value = environment.getProperty(envVarName, String.class);
        if (value.isPresent()) {
            return true;
        }
        
        // Test 3 : Toutes les variantes
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            value = environment.getProperty(variant, String.class);
            if (value.isPresent()) {
                return true;
            }
        }
        
        return false;
    }
    
    public String getPropertyValue(String propertyName) {
        Optional<String> value = environment.getProperty(propertyName, String.class);
        if (value.isPresent()) {
            return value.get();
        }
        
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        value = environment.getProperty(envVarName, String.class);
        if (value.isPresent()) {
            return value.get();
        }
        
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            value = environment.getProperty(variant, String.class);
            if (value.isPresent()) {
                return value.get();
            }
        }
        
        return null;
    }
    
    public String findActualPropertyName(String propertyName) {
        if (environment.getProperty(propertyName, String.class).isPresent()) {
            return propertyName;
        }
        
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        if (environment.getProperty(envVarName, String.class).isPresent()) {
            return envVarName;
        }
        
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            if (environment.getProperty(variant, String.class).isPresent()) {
                return variant;
            }
        }
        
        return null;
    }
    
    public String generateSuggestion(String propertyName) {
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        
        return String.format(
            "Add to application.yml: %s: <value>\n" +
            "       OR set environment variable: export %s=<value>",
            propertyName, envVarName
        );
    }
    
    public PropertyBindingResolver getBaseResolver() {
        return baseResolver;
    }
}
