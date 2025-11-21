package com.mycompany.validator.core.formatter;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;

import java.util.List;
import java.util.Map;

/**
 * Formatte les erreurs de validation de manière lisible et esthétique.
 * Masque automatiquement les valeurs sensibles (mots de passe, secrets, clés API).
 */
public class BeautifulErrorFormatter {
    
    private static final String SEPARATOR = "═══════════════════════════════════════════════════════════════════════════════";
    private static final String LINE = "───────────────────────────────────────────────────────────────────────────────";
    private static final String DOUBLE_LINE = "╔═══════════════════════════════════════════════════════════════════════════════╗";
    private static final String DOUBLE_LINE_END = "╚═══════════════════════════════════════════════════════════════════════════════╝";
    private static final String BOX_SIDE = "║";
    
    private final SecretDetector secretDetector;
    
    public BeautifulErrorFormatter() {
        this.secretDetector = new SecretDetector();
    }
    
    /**
     * Formatte le résultat de validation en une chaîne lisible.
     * Utilise un format "box" amélioré avec des bordures doubles.
     * 
     * @param result Résultat de validation
     * @return Chaîne formatée
     */
    public String format(ValidationResult result) {
        if (result.isValid()) {
            return formatSuccess();
        }
        
        StringBuilder sb = new StringBuilder();
        
        // En-tête avec box amélioré
        sb.append("\n").append(DOUBLE_LINE).append("\n");
        sb.append(BOX_SIDE).append("                                                                               ").append(BOX_SIDE).append("\n");
        sb.append(BOX_SIDE).append("              ⚠️   CONFIGURATION VALIDATION FAILED   ⚠️                        ").append(BOX_SIDE).append("\n");
        sb.append(BOX_SIDE).append("                                                                               ").append(BOX_SIDE).append("\n");
        sb.append(DOUBLE_LINE_END).append("\n\n");
        
        // Résumé
        sb.append(formatSummary(result));
        
        // Erreurs par type
        Map<ErrorType, List<ConfigurationError>> errorsByType = result.getErrorsGroupedByType();
        for (Map.Entry<ErrorType, List<ConfigurationError>> entry : errorsByType.entrySet()) {
            sb.append(formatErrorSection(entry.getKey(), entry.getValue()));
        }
        
        // Pied de page
        sb.append(SEPARATOR).append("\n\n");
        sb.append("  📝 ACTION REQUIRED\n");
        sb.append(LINE).append("\n\n");
        sb.append("  Fix the errors above to start your application.\n");
        sb.append("  💡 TIP: Fix all errors at once to avoid multiple restarts!\n");
        sb.append(LINE).append("\n");
        
        return sb.toString();
    }
    
    private String formatSuccess() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(SEPARATOR).append("\n");
        sb.append("  ✅  CONFIGURATION VALIDATION PASSED  ✅\n");
        sb.append(SEPARATOR).append("\n\n");
        sb.append("  All configuration properties are valid!\n\n");
        return sb.toString();
    }
    
    private String formatSummary(ValidationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("  📊 Summary:\n");
        sb.append("     Total Errors: ").append(result.getErrorCount()).append("\n\n");
        
        Map<ErrorType, Long> countByType = result.getErrorCountByType();
        for (Map.Entry<ErrorType, Long> entry : countByType.entrySet()) {
            String icon = getIconForErrorType(entry.getKey());
            sb.append("     ").append(icon).append(" ")
              .append(entry.getKey().getDisplayName()).append(": ")
              .append(entry.getValue()).append("\n");
        }
        sb.append("\n");
        
        return sb.toString();
    }
    
    private String formatErrorSection(ErrorType type, List<ConfigurationError> errors) {
        StringBuilder sb = new StringBuilder();
        
        String icon = getIconForErrorType(type);
        sb.append(LINE).append("\n");
        sb.append("  ").append(icon).append("  ").append(type.getDisplayName()).append("\n");
        sb.append(LINE).append("\n\n");
        
        for (ConfigurationError error : errors) {
            sb.append(formatError(error));
        }
        
        return sb.toString();
    }
    
    private String formatError(ConfigurationError error) {
        StringBuilder sb = new StringBuilder();
        
        // Nom de la propriété
        if (error.getPropertyName() != null && !error.getPropertyName().isEmpty()) {
            sb.append("  ● Property: ").append(error.getPropertyName());
            
            // Ajouter un badge si sensible
            if (error.isSensitive()) {
                sb.append(" 🔒 [SENSITIVE]");
            }
            sb.append("\n");
        }
        
        // Source
        if (error.getSource() != null) {
            sb.append("    Source: ").append(error.getSource().getName()).append("\n");
        }
        
        // Message d'erreur (sanitisé si sensible)
        String errorMessage = error.getErrorMessage();
        if (error.isSensitive()) {
            errorMessage = secretDetector.sanitizeErrorMessage(error.getPropertyName(), errorMessage);
        }
        sb.append("    ❌ ").append(errorMessage).append("\n");
        
        // Suggestion
        if (error.getSuggestion() != null && !error.getSuggestion().isEmpty()) {
            String[] suggestionLines = error.getSuggestion().split("\n");
            sb.append("    💡 ").append(suggestionLines[0]).append("\n");
            for (int i = 1; i < suggestionLines.length; i++) {
                sb.append("       ").append(suggestionLines[i]).append("\n");
            }
        }
        
        sb.append("\n");
        return sb.toString();
    }
    
    private String getIconForErrorType(ErrorType type) {
        switch (type) {
            case MISSING_PROPERTY:
                return "🔴";
            case EMPTY_VALUE:
                return "⚪";
            case UNRESOLVED_PLACEHOLDER:
                return "🔶";
            case IMPORT_FILE_INACCESSIBLE:
                return "📁";
            case IMPORT_FILE_INVALID_FORMAT:
                return "📄";
            case INVALID_VALUE_FORMAT:
                return "⚠️";
            case CIRCULAR_REFERENCE:
                return "🔄";
            default:
                return "❓";
        }
    }
    
    /**
     * Formatte une liste d'erreurs en format compact (une ligne par erreur).
     * 
     * @param result Résultat de validation
     * @return Chaîne formatée compacte
     */
    public String formatCompact(ValidationResult result) {
        if (result.isValid()) {
            return "✅ Configuration validation passed";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("❌ Configuration validation failed with ")
          .append(result.getErrorCount())
          .append(" error(s):\n");
        
        for (ConfigurationError error : result.getErrors()) {
            sb.append("  - [").append(error.getType().getDisplayName()).append("] ");
            if (error.getPropertyName() != null) {
                sb.append(error.getPropertyName()).append(": ");
            }
            sb.append(error.getErrorMessage()).append("\n");
        }
        
        return sb.toString();
    }
}
