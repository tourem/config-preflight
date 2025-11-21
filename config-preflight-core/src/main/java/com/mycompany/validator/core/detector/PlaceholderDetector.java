package com.mycompany.validator.core.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Détecte les placeholders dans les valeurs de propriétés.
 * Supporte les formats : ${property}, ${property:defaultValue}, #{expression}
 */
public class PlaceholderDetector {
    
    // Pattern pour détecter ${...}
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?\\}");
    
    // Pattern pour détecter #{...} (Spring Expression Language)
    private static final Pattern SPEL_PATTERN = Pattern.compile("#\\{([^}]+)\\}");
    
    /**
     * Détecte tous les placeholders dans une valeur.
     * 
     * @param value Valeur à analyser
     * @return Liste des noms de propriétés référencées
     */
    public List<String> detectPlaceholders(String value) {
        List<String> placeholders = new ArrayList<>();
        
        if (value == null || value.isEmpty()) {
            return placeholders;
        }
        
        // Détecter ${property} et ${property:default}
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        while (matcher.find()) {
            String propertyName = matcher.group(1).trim();
            if (!propertyName.isEmpty()) {
                placeholders.add(propertyName);
            }
        }
        
        return placeholders;
    }
    
    /**
     * Détecte uniquement les placeholders SANS valeur par défaut.
     * Ces placeholders sont obligatoires et doivent être définis.
     * 
     * Exemples :
     * - ${DATABASE_URL} → Retourné (obligatoire)
     * - ${DATABASE_URL:jdbc:h2:mem} → Non retourné (a une valeur par défaut)
     * 
     * @param value Valeur à analyser
     * @return Liste des noms de propriétés obligatoires
     */
    public List<String> detectRequiredPlaceholders(String value) {
        List<String> requiredPlaceholders = new ArrayList<>();
        
        if (value == null || value.isEmpty()) {
            return requiredPlaceholders;
        }
        
        // Détecter ${property} et ${property:default}
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        while (matcher.find()) {
            String propertyName = matcher.group(1).trim();
            String defaultValue = matcher.group(2); // Peut être null
            
            // Ajouter uniquement si pas de valeur par défaut
            if (!propertyName.isEmpty() && defaultValue == null) {
                requiredPlaceholders.add(propertyName);
            }
        }
        
        return requiredPlaceholders;
    }
    
    /**
     * Vérifie si une valeur contient des placeholders.
     * 
     * @param value Valeur à vérifier
     * @return true si la valeur contient au moins un placeholder
     */
    public boolean hasPlaceholders(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return PLACEHOLDER_PATTERN.matcher(value).find();
    }
    
    /**
     * Vérifie si une valeur contient des expressions SpEL.
     * 
     * @param value Valeur à vérifier
     * @return true si la valeur contient au moins une expression SpEL
     */
    public boolean hasSpelExpressions(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return SPEL_PATTERN.matcher(value).find();
    }
    
    /**
     * Extrait la valeur par défaut d'un placeholder.
     * 
     * @param placeholder Placeholder complet (ex: "${db.url:jdbc:h2:mem}")
     * @return Valeur par défaut ou null si non définie
     */
    public String extractDefaultValue(String placeholder) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeholder);
        if (matcher.find()) {
            String defaultValue = matcher.group(2);
            return defaultValue != null ? defaultValue.trim() : null;
        }
        return null;
    }
    
    /**
     * Vérifie si un placeholder a une valeur par défaut.
     * 
     * @param placeholder Placeholder à vérifier
     * @return true si le placeholder a une valeur par défaut
     */
    public boolean hasDefaultValue(String placeholder) {
        return extractDefaultValue(placeholder) != null;
    }
}
