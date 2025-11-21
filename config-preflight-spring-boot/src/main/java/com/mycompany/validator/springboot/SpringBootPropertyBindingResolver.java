package com.mycompany.validator.springboot;

import com.mycompany.validator.core.detector.PropertyBindingResolver;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * Résout les bindings de propriétés pour Spring Boot.
 * Vérifie si une propriété existe sous n'importe quelle forme supportée par Spring Boot.
 */
public class SpringBootPropertyBindingResolver {
    
    private final Environment environment;
    private final PropertyBindingResolver baseResolver;
    
    public SpringBootPropertyBindingResolver(Environment environment) {
        this.environment = environment;
        this.baseResolver = new PropertyBindingResolver();
    }
    
    /**
     * Vérifie si une propriété existe dans l'environnement Spring Boot,
     * en testant TOUTES les variantes possibles.
     * 
     * Pour "app.database.url", vérifie :
     * 1. app.database.url (property directe)
     * 2. APP_DATABASE_URL (env var)
     * 3. app_database_url (env var lowercase)
     * 4. appDatabaseUrl (camelCase)
     * 5. app-database-url (kebab-case)
     * 
     * @param propertyName Nom de la propriété à chercher
     * @return true si la propriété existe sous n'importe quelle forme
     */
    public boolean propertyExists(String propertyName) {
        // Spring Boot résout automatiquement les variantes via son PropertyResolver
        // On peut donc utiliser directement environment.containsProperty()
        // qui gère déjà app.database.url → APP_DATABASE_URL
        
        // Test 1 : Nom original
        if (environment.containsProperty(propertyName)) {
            return true;
        }
        
        // Test 2 : Variable d'environnement (convention principale)
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        if (environment.containsProperty(envVarName)) {
            return true;
        }
        
        // Test 3 : Toutes les autres variantes
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            if (environment.containsProperty(variant)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Récupère la valeur d'une propriété en testant toutes les variantes.
     * 
     * @param propertyName Nom de la propriété
     * @return Valeur de la propriété, ou null si elle n'existe pas
     */
    public String getPropertyValue(String propertyName) {
        // Spring Boot gère automatiquement les variantes
        String value = environment.getProperty(propertyName);
        if (value != null) {
            return value;
        }
        
        // Tester explicitement la variante env var si Spring Boot ne l'a pas trouvée
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        value = environment.getProperty(envVarName);
        if (value != null) {
            return value;
        }
        
        // Tester toutes les autres variantes
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            value = environment.getProperty(variant);
            if (value != null) {
                return value;
            }
        }
        
        return null;
    }
    
    /**
     * Trouve la source réelle d'une propriété (sous quelle forme elle existe).
     * 
     * @param propertyName Nom de la propriété
     * @return Le nom sous lequel la propriété a été trouvée, ou null
     */
    public String findActualPropertyName(String propertyName) {
        if (environment.containsProperty(propertyName)) {
            return propertyName;
        }
        
        String envVarName = baseResolver.toEnvironmentVariableName(propertyName);
        if (environment.containsProperty(envVarName)) {
            return envVarName;
        }
        
        List<String> variants = baseResolver.getAllVariants(propertyName);
        for (String variant : variants) {
            if (environment.containsProperty(variant)) {
                return variant;
            }
        }
        
        return null;
    }
    
    /**
     * Génère un message d'aide pour définir une propriété manquante.
     */
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
