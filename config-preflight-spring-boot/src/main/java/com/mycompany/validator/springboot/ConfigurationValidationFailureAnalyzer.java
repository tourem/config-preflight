package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * FailureAnalyzer pour Spring Boot qui affiche les erreurs de validation
 * de manière propre, sans stacktrace illisible.
 * 
 * Spring Boot utilise automatiquement ce FailureAnalyzer pour formater
 * les exceptions ConfigurationValidationException.
 */
public class ConfigurationValidationFailureAnalyzer 
        extends AbstractFailureAnalyzer<SpringBootValidatorEnvironmentPostProcessor.ConfigurationValidationException> {
    
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, 
            SpringBootValidatorEnvironmentPostProcessor.ConfigurationValidationException cause) {
        
        ValidationResult result = cause.getValidationResult();
        
        // Description : le message formaté avec toutes les erreurs
        String description = formatter.format(result);
        
        // Action : ce que l'utilisateur doit faire
        String action = buildActionMessage(result);
        
        return new FailureAnalysis(description, action, cause);
    }
    
    private String buildActionMessage(ValidationResult result) {
        StringBuilder action = new StringBuilder();
        
        action.append("Fix the configuration errors above:\n\n");
        
        int errorCount = result.getErrorCount();
        if (errorCount == 1) {
            action.append("1 property needs to be configured.\n");
        } else {
            action.append(errorCount).append(" properties need to be configured.\n");
        }
        
        action.append("\nYou can:\n");
        action.append("  1. Add the properties to application.properties or application.yml\n");
        action.append("  2. Set them as environment variables\n");
        action.append("  3. Pass them as command-line arguments: --property.name=value\n");
        
        action.append("\n💡 TIP: Fix all errors at once to avoid multiple restarts!");
        
        return action.toString();
    }
}
