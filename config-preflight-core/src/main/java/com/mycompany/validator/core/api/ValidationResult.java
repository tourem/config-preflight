package com.mycompany.validator.core.api;

import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Résultat d'une validation de configuration.
 * Contient toutes les erreurs trouvées et des méthodes utilitaires.
 */
public class ValidationResult {
    
    private final List<ConfigurationError> errors;
    
    public ValidationResult(List<ConfigurationError> errors) {
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }
    
    /**
     * @return true si aucune erreur n'a été trouvée
     */
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    /**
     * @return true si des erreurs ont été trouvées
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * @return Liste de toutes les erreurs
     */
    public List<ConfigurationError> getErrors() {
        return new ArrayList<>(errors);
    }
    
    /**
     * @return Nombre total d'erreurs
     */
    public int getErrorCount() {
        return errors.size();
    }
    
    /**
     * Filtre les erreurs par type.
     * 
     * @param type Type d'erreur à filtrer
     * @return Liste des erreurs du type spécifié
     */
    public List<ConfigurationError> getErrorsByType(ErrorType type) {
        return errors.stream()
                .filter(error -> error.getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Groupe les erreurs par type.
     * 
     * @return Map des erreurs groupées par type
     */
    public Map<ErrorType, List<ConfigurationError>> getErrorsGroupedByType() {
        return errors.stream()
                .collect(Collectors.groupingBy(ConfigurationError::getType));
    }
    
    /**
     * @return Nombre d'erreurs par type
     */
    public Map<ErrorType, Long> getErrorCountByType() {
        return errors.stream()
                .collect(Collectors.groupingBy(
                        ConfigurationError::getType,
                        Collectors.counting()
                ));
    }
    
    /**
     * Combine ce résultat avec un autre.
     * 
     * @param other Autre résultat à combiner
     * @return Nouveau résultat contenant toutes les erreurs
     */
    public ValidationResult merge(ValidationResult other) {
        List<ConfigurationError> allErrors = new ArrayList<>(this.errors);
        allErrors.addAll(other.errors);
        return new ValidationResult(allErrors);
    }
    
    @Override
    public String toString() {
        if (isValid()) {
            return "ValidationResult: SUCCESS (no errors)";
        }
        return String.format("ValidationResult: FAILED (%d errors)", getErrorCount());
    }
}
