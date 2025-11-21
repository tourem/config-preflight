package com.mycompany.validator.springboot;

import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

/**
 * Résolveur de propriétés utilisant l'API Binder de Spring Boot.
 * 
 * Cette approche est 100% fidèle au framework car elle utilise exactement
 * la même logique que Spring Boot pour résoudre les propriétés.
 * 
 * Avantages par rapport au binding manuel :
 * - Utilise la même logique que @ConfigurationProperties
 * - Gère automatiquement toutes les variantes (kebab-case, camelCase, etc.)
 * - Supporte les conversions de types
 * - Respecte les relaxed binding rules de Spring Boot
 */
public class SpringBootBinderPropertyResolver {
    
    private final Binder binder;
    
    public SpringBootBinderPropertyResolver(Environment environment) {
        this.binder = Binder.get(environment);
    }
    
    /**
     * Vérifie si une propriété existe en utilisant l'API Binder.
     * 
     * Cette méthode utilise exactement la même logique que Spring Boot
     * pour résoudre les propriétés, incluant :
     * - Relaxed binding (app.database.url, app.databaseUrl, APP_DATABASE_URL)
     * - Conversions de types
     * - Valeurs par défaut
     * 
     * @param propertyName Nom de la propriété
     * @return true si la propriété peut être bindée
     */
    public boolean propertyExists(String propertyName) {
        try {
            BindResult<String> result = binder.bind(propertyName, String.class);
            return result.isBound();
        } catch (Exception e) {
            // En cas d'erreur de binding, considérer que la propriété n'existe pas
            return false;
        }
    }
    
    /**
     * Récupère la valeur d'une propriété en utilisant l'API Binder.
     * 
     * @param propertyName Nom de la propriété
     * @return Valeur de la propriété, ou null si elle n'existe pas
     */
    public String getPropertyValue(String propertyName) {
        try {
            BindResult<String> result = binder.bind(propertyName, String.class);
            return result.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Vérifie si une propriété existe avec une valeur non vide.
     * 
     * @param propertyName Nom de la propriété
     * @return true si la propriété existe et n'est pas vide
     */
    public boolean hasNonEmptyValue(String propertyName) {
        String value = getPropertyValue(propertyName);
        return value != null && !value.trim().isEmpty();
    }
    
    /**
     * Tente de binder une propriété avec un type spécifique.
     * Utile pour valider que la propriété peut être convertie au type attendu.
     * 
     * @param propertyName Nom de la propriété
     * @param type Type cible
     * @param <T> Type générique
     * @return true si le binding réussit
     */
    public <T> boolean canBindAs(String propertyName, Class<T> type) {
        try {
            BindResult<T> result = binder.bind(propertyName, type);
            return result.isBound();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Récupère la valeur bindée avec un type spécifique.
     * 
     * @param propertyName Nom de la propriété
     * @param type Type cible
     * @param <T> Type générique
     * @return Valeur bindée, ou null si le binding échoue
     */
    public <T> T getBindedValue(String propertyName, Class<T> type) {
        try {
            BindResult<T> result = binder.bind(propertyName, type);
            return result.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
