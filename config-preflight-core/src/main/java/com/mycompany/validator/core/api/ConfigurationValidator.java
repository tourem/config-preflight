package com.mycompany.validator.core.api;

/**
 * Interface principale pour la validation de configuration.
 * Implémentée par chaque framework (Spring Boot, Quarkus, Micronaut).
 */
public interface ConfigurationValidator {
    
    /**
     * Valide toutes les propriétés de configuration chargées.
     * Détecte les placeholders non résolus, les fichiers importés manquants, etc.
     * 
     * @return Résultat de validation contenant toutes les erreurs trouvées
     */
    ValidationResult validateAll();
    
    /**
     * Valide que les propriétés requises sont définies.
     * Vérifie toutes les variantes possibles (property, env var, etc.)
     * 
     * @param requiredProperties Liste des propriétés requises
     * @return Résultat de validation
     */
    ValidationResult validateRequired(String... requiredProperties);
    
    /**
     * Valide tous les placeholders (${...}) dans les valeurs de propriétés.
     * Vérifie que chaque placeholder peut être résolu.
     * 
     * @return Résultat de validation
     */
    ValidationResult validatePlaceholders();
}
