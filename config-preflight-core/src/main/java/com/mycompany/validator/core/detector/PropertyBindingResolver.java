package com.mycompany.validator.core.detector;

import java.util.ArrayList;
import java.util.List;

/**
 * Résout les différentes variantes de noms de propriétés selon les conventions
 * de binding des frameworks (Spring Boot, Quarkus, Micronaut).
 * 
 * Convention : app.database.url <-> APP_DATABASE_URL
 */
public class PropertyBindingResolver {
    
    /**
     * Convertit un nom de propriété en nom de variable d'environnement.
     * 
     * Exemples :
     * - app.database.url → APP_DATABASE_URL
     * - my-app.redis-host → MY_APP_REDIS_HOST
     * - spring.datasource.url → SPRING_DATASOURCE_URL
     * 
     * @param propertyName Nom de la propriété (ex: "app.database.url")
     * @return Nom de variable d'environnement (ex: "APP_DATABASE_URL")
     */
    public String toEnvironmentVariableName(String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return propertyName;
        }
        
        return propertyName
            .replace('.', '_')      // app.database.url → app_database_url
            .replace('-', '_')      // my-app → my_app
            .toUpperCase();         // app_database_url → APP_DATABASE_URL
    }
    
    /**
     * Convertit un nom de variable d'environnement en nom de propriété.
     * 
     * Exemples :
     * - APP_DATABASE_URL → app.database.url
     * - MY_APP_REDIS_HOST → my.app.redis.host
     * 
     * @param envVarName Nom de la variable d'environnement
     * @return Nom de propriété
     */
    public String toPropertyName(String envVarName) {
        if (envVarName == null || envVarName.isEmpty()) {
            return envVarName;
        }
        
        return envVarName
            .replace('_', '.')      // APP_DATABASE_URL → APP.DATABASE.URL
            .toLowerCase();         // APP.DATABASE.URL → app.database.url
    }
    
    /**
     * Génère toutes les variantes possibles d'un nom de propriété.
     * 
     * Pour "app.database.url", retourne :
     * - app.database.url (original)
     * - APP_DATABASE_URL (env var uppercase)
     * - app_database_url (env var lowercase)
     * - appDatabaseUrl (camelCase)
     * - app-database-url (kebab-case)
     * 
     * @param propertyName Nom de la propriété
     * @return Liste de toutes les variantes
     */
    public List<String> getAllVariants(String propertyName) {
        List<String> variants = new ArrayList<>();
        
        // Original
        variants.add(propertyName);
        
        // Environment variable uppercase (convention principale)
        variants.add(toEnvironmentVariableName(propertyName));
        
        // Environment variable lowercase
        variants.add(propertyName.replace('.', '_').replace('-', '_').toLowerCase());
        
        // CamelCase (moins courant mais supporté par certains frameworks)
        variants.add(toCamelCase(propertyName));
        
        // Kebab-case (convention YAML)
        variants.add(toKebabCase(propertyName));
        
        return variants;
    }
    
    /**
     * Convertit en camelCase.
     * app.database.url → appDatabaseUrl
     */
    private String toCamelCase(String propertyName) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        
        for (char c : propertyName.toCharArray()) {
            if (c == '.' || c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Convertit en kebab-case.
     * app.database.url → app-database-url
     */
    private String toKebabCase(String propertyName) {
        return propertyName
            .replace('.', '-')
            .replace('_', '-')
            .toLowerCase();
    }
    
    /**
     * Vérifie si un nom ressemble à une variable d'environnement.
     * 
     * @param name Nom à vérifier
     * @return true si c'est probablement une env var (UPPER_CASE_WITH_UNDERSCORES)
     */
    public boolean looksLikeEnvironmentVariable(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        // Une env var typique est en UPPERCASE avec des underscores
        return name.equals(name.toUpperCase()) && name.contains("_");
    }
    
    /**
     * Vérifie si un nom ressemble à une propriété.
     * 
     * @param name Nom à vérifier
     * @return true si c'est probablement une propriété (lowercase.with.dots)
     */
    public boolean looksLikeProperty(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        // Une propriété typique est en lowercase avec des points ou tirets
        return !name.equals(name.toUpperCase()) && (name.contains(".") || name.contains("-"));
    }
}
