package com.mycompany.validator.core.formatter;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;

import java.util.List;

/**
 * Formatte les erreurs de validation de manière lisible et esthétique.
 * Masque automatiquement les valeurs sensibles (mots de passe, secrets, clés API).
 */
public class BeautifulErrorFormatter {
    
    private static final int BOX_WIDTH = 80;
    private static final String TOP_LINE = "╔══════════════════════════════════════════════════════════════════════════════╗";
    private static final String MIDDLE_LINE = "╠══════════════════════════════════════════════════════════════════════════════╣";
    private static final String BOTTOM_LINE = "╚══════════════════════════════════════════════════════════════════════════════╝";
    private static final String BOX_SIDE = "║";
    
    private final SecretDetector secretDetector;
    
    public BeautifulErrorFormatter() {
        this.secretDetector = new SecretDetector();
    }
    
    /**
     * Formatte le résultat de validation en une chaîne lisible.
     * Utilise un format "box" avec des bordures doubles.
     * 
     * @param result Résultat de validation
     * @return Chaîne formatée
     */
    public String format(ValidationResult result) {
        if (result.isValid()) {
            return formatSuccess();
        }
        
        StringBuilder sb = new StringBuilder();
        
        // En-tête
        sb.append("\n").append(TOP_LINE).append("\n");
        sb.append(formatBoxLine("❌  CONFIGURATION VALIDATION FAILED  ❌", true)).append("\n");
        sb.append(MIDDLE_LINE).append("\n");
        sb.append(formatBoxLine("", false)).append("\n");
        
        // Chaque erreur
        for (ConfigurationError error : result.getErrors()) {
            sb.append(formatError(error));
        }
        
        // Ligne vide finale
        sb.append(formatBoxLine("", false)).append("\n");
        sb.append(BOTTOM_LINE).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Formatte une ligne dans la box avec padding approprié.
     */
    private String formatBoxLine(String content, boolean center) {
        int contentLength = content.length();
        int padding = BOX_WIDTH - 2 - contentLength; // -2 pour les bordures
        
        if (center) {
            int leftPad = padding / 2;
            int rightPad = padding - leftPad;
            return BOX_SIDE + " ".repeat(leftPad) + content + " ".repeat(rightPad) + BOX_SIDE;
        } else {
            return BOX_SIDE + " ".repeat(BOX_WIDTH - 2) + BOX_SIDE;
        }
    }
    
    private String formatSuccess() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(TOP_LINE).append("\n");
        sb.append(formatBoxLine("✅  CONFIGURATION VALIDATION PASSED  ✅", true)).append("\n");
        sb.append(BOTTOM_LINE).append("\n");
        return sb.toString();
    }
    
    
    private String formatError(ConfigurationError error) {
        StringBuilder sb = new StringBuilder();
        
        // Ligne: 👉 Property: xxx
        String propertyLine = "👉 Property: " + (error.getPropertyName() != null ? error.getPropertyName() : "unknown");
        sb.append(formatBoxLine(propertyLine, false, 3)).append("\n");
        
        // Ligne: Source: xxx
        String sourceName = error.getSource() != null ? error.getSource().getName() : "unknown";
        String sourceLine = "   Source:   " + sourceName;
        sb.append(formatBoxLine(sourceLine, false, 3)).append("\n");
        
        // Ligne: Error: xxx
        String errorMessage = error.getErrorMessage();
        if (error.isSensitive()) {
            errorMessage = secretDetector.sanitizeErrorMessage(error.getPropertyName(), errorMessage);
        }
        String errorLine = "   Error:    " + errorMessage;
        sb.append(formatBoxLine(errorLine, false, 3)).append("\n");
        
        // Ligne: 💡 Fix: xxx
        if (error.getSuggestion() != null && !error.getSuggestion().isEmpty()) {
            String[] suggestionLines = error.getSuggestion().split("\n");
            String fixLine = "   💡 Fix:   " + suggestionLines[0];
            sb.append(formatBoxLine(fixLine, false, 3)).append("\n");
            
            // Lignes supplémentaires de suggestion
            for (int i = 1; i < suggestionLines.length; i++) {
                String additionalLine = "             " + suggestionLines[i].trim();
                sb.append(formatBoxLine(additionalLine, false, 3)).append("\n");
            }
        }
        
        // Ligne vide entre les erreurs
        sb.append(formatBoxLine("", false)).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Formatte une ligne dans la box avec un contenu aligné à gauche et padding.
     */
    private String formatBoxLine(String content, boolean center, int leftPadding) {
        // Calculer l'espace disponible (80 - 2 bordures - padding gauche)
        int availableWidth = BOX_WIDTH - 2 - leftPadding;
        
        // Tronquer le contenu si trop long
        String displayContent = content;
        if (content.length() > availableWidth) {
            displayContent = content.substring(0, availableWidth - 3) + "...";
        }
        
        // Calculer le padding à droite
        int rightPadding = availableWidth - displayContent.length();
        
        return BOX_SIDE + " ".repeat(leftPadding) + displayContent + " ".repeat(rightPadding) + BOX_SIDE;
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
