package com.mycompany.validator.core.model;

/**
 * Types d'erreurs de configuration possibles.
 */
public enum ErrorType {
    
    /**
     * Une propriété requise n'est pas définie.
     */
    MISSING_PROPERTY("Missing Property"),
    
    /**
     * Une propriété est définie mais a une valeur vide.
     */
    EMPTY_VALUE("Empty Value"),
    
    /**
     * Un placeholder ${...} ne peut pas être résolu.
     */
    UNRESOLVED_PLACEHOLDER("Unresolved Placeholder"),
    
    /**
     * Un fichier importé (via spring.config.import, etc.) n'est pas accessible.
     */
    IMPORT_FILE_INACCESSIBLE("Import File Inaccessible"),
    
    /**
     * Un fichier importé a un format invalide.
     */
    IMPORT_FILE_INVALID_FORMAT("Import File Invalid Format"),
    
    /**
     * Une valeur de propriété a un format invalide.
     */
    INVALID_VALUE_FORMAT("Invalid Value Format"),
    
    /**
     * Une propriété référence une autre propriété qui n'existe pas.
     */
    CIRCULAR_REFERENCE("Circular Reference"),
    
    /**
     * Erreur générique.
     */
    UNKNOWN("Unknown Error");
    
    private final String displayName;
    
    ErrorType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
