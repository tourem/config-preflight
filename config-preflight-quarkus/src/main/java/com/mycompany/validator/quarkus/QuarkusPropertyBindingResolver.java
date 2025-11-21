package com.mycompany.validator.quarkus;

import com.mycompany.validator.core.detector.PropertyBindingResolver;
import org.eclipse.microprofile.config.Config;

import java.util.List;
import java.util.Optional;

/**
 * Résout les bindings de propriétés pour Quarkus.
 */
public class QuarkusPropertyBindingResolver {
    
    private final Config config;
    private final PropertyBindingResolver baseResolver;
    
    public QuarkusPropertyBindingResolver(Config config) {
        this.config = config;
        this.baseResolver = new PropertyBindingResolver();
    }
    
    public boolean propertyExists(String propertyName) {
        // Test 1 : Nom original
        Optional<String> value = config.getOptionalValue(propertyName, String.class);
        if (value.isPresent()) {
            return true;
        }
        
        // Test 2 : Variable d'environnement
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        value = config.getOptionalValue(envVarName, String.class);
        if (value.isPresent()) {
            return true;
        }
        
        // Test 3 : Toutes les variantes
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            value = config.getOptionalValue(variant, String.class);
            if (value.isPresent()) {
                return true;
            }
        }
        
        return false;
    }
    
    public String getPropertyValue(String propertyName) {
        Optional<String> value = config.getOptionalValue(propertyName, String.class);
        if (value.isPresent()) {
            return value.get();
        }
        
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        value = config.getOptionalValue(envVarName, String.class);
        if (value.isPresent()) {
            return value.get();
        }
        
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            value = config.getOptionalValue(variant, String.class);
            if (value.isPresent()) {
                return value.get();
            }
        }
        
        return null;
    }
    
    public String findActualPropertyName(String propertyName) {
        if (config.getOptionalValue(propertyName, String.class).isPresent()) {
            return propertyName;
        }
        
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        if (config.getOptionalValue(envVarName, String.class).isPresent()) {
            return envVarName;
        }
        
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            if (config.getOptionalValue(variant, String.class).isPresent()) {
                return variant;
            }
        }
        
        return null;
    }
    
    public String generateSuggestion(String propertyName) {
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        
        return String.format(
            "Add to application.properties: %s=<value>\n" +
            "       OR set environment variable: export %s=<value>",
            propertyName, envVarName
        );
    }
    
    public PropertyBindingResolver getBaseResolver() {
        return baseResolver;
    }
}
