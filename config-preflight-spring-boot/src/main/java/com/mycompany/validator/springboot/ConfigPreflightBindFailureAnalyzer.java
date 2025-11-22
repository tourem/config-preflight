package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import com.mycompany.validator.core.model.PropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;

/**
 * FailureAnalyzer qui intercepte les erreurs de @ConfigurationProperties + @Validated
 * et affiche TOUTES les erreurs avec le beau formatage de config-preflight.
 * 
 * Ce FailureAnalyzer s'exécute quand Spring Boot échoue au binding à cause de
 * contraintes de validation (@NotNull, @NotEmpty, etc.).
 * 
 * Au lieu d'afficher les erreurs une par une, il extrait TOUTES les erreurs
 * et les affiche avec BeautifulErrorFormatter.
 * 
 * @Order(Ordered.HIGHEST_PRECEDENCE) ensures this runs BEFORE Spring Boot's default BindFailureAnalyzer
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConfigPreflightBindFailureAnalyzer extends AbstractFailureAnalyzer<BindException> {
    
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, BindException cause) {
        // Vérifier si c'est une erreur de validation
        if (cause.getCause() instanceof BindValidationException) {
            BindValidationException validationException = (BindValidationException) cause.getCause();
            
            // Extraire TOUTES les erreurs de validation
            List<ConfigurationError> errors = extractAllErrors(validationException, cause);
            
            if (!errors.isEmpty()) {
                // Formater avec BeautifulErrorFormatter
                ValidationResult result = new ValidationResult(errors);
                String description = formatter.format(result);
                
                // Générer les actions
                String action = buildAction(errors);
                
                return new FailureAnalysis(description, action, cause);
            }
        }
        
        // Si ce n'est pas une erreur de validation, laisser le FailureAnalyzer par défaut gérer
        return null;
    }
    
    /**
     * Extrait toutes les erreurs de validation de la BindValidationException.
     */
    private List<ConfigurationError> extractAllErrors(BindValidationException validationException, BindException bindException) {
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Récupérer tous les ObjectError (qui incluent les FieldError)
        List<ObjectError> allErrors = validationException.getValidationErrors().getAllErrors();
        
        for (ObjectError error : allErrors) {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                
                // Construire le nom complet de la propriété
                String propertyName = buildPropertyName(bindException, fieldError);
                
                // Détecter si c'est une propriété sensible
                boolean isSensitive = secretDetector.isSensitive(propertyName);
                
                // Détecter la source réelle
                PropertySource source = detectPropertySource();
                
                // Créer le ConfigurationError
                errors.add(ConfigurationError.builder()
                    .type(ErrorType.MISSING_PROPERTY)
                    .propertyName(propertyName)
                    .source(source)
                    .errorMessage(fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Property '" + propertyName + "' is not set")
                    .suggestion(generateSuggestion(propertyName))
                    .isSensitive(isSensitive)
                    .build());
            }
        }
        
        return errors;
    }
    
    /**
     * Construit le nom complet de la propriété à partir du prefix et du field name.
     */
    private String buildPropertyName(BindException bindException, FieldError fieldError) {
        // Extraire le prefix de la BindException
        String prefix = extractPrefix(bindException);
        
        // Le fieldError.getField() contient déjà le nom du champ
        String fieldName = fieldError.getField();
        
        // Construire le nom complet avec le prefix
        if (prefix != null && !prefix.isEmpty()) {
            // Convertir le field name en kebab-case
            String kebabCaseField = convertToKebabCase(fieldName);
            return prefix + "." + kebabCaseField;
        }
        
        // Si pas de prefix, juste le field name en kebab-case
        return convertToKebabCase(fieldName);
    }
    
    /**
     * Extrait le prefix de @ConfigurationProperties de la BindException.
     */
    private String extractPrefix(BindException bindException) {
        // Essayer d'extraire depuis le ConfigurationPropertyName si disponible
        if (bindException.getProperty() != null) {
            ConfigurationPropertyName propertyName = bindException.getProperty().getName();
            if (propertyName != null) {
                // Le nom peut être "database" ou vide
                String name = propertyName.toString();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        }
        
        // Fallback: extraire depuis le message
        String message = bindException.getMessage();
        
        // Format: "Failed to bind properties under 'database' to ..."
        if (message != null && message.contains("under '")) {
            int startIndex = message.indexOf("under '") + 7;
            int endIndex = message.indexOf("'", startIndex);
            if (endIndex > startIndex) {
                return message.substring(startIndex, endIndex).trim();
            }
        }
        
        // Autre format: "prefix=database"
        if (message != null && message.contains("prefix=")) {
            int startIndex = message.indexOf("prefix=") + 7;
            int endIndex = message.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = message.indexOf(")", startIndex);
            }
            if (endIndex > startIndex) {
                return message.substring(startIndex, endIndex).trim();
            }
        }
        
        return "";
    }
    
    /**
     * Convertit un nom camelCase en kebab-case.
     */
    private String convertToKebabCase(String camelCase) {
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
    
    /**
     * Génère une suggestion pour corriger l'erreur.
     */
    private String generateSuggestion(String propertyName) {
        String envVarName = propertyName.replace('.', '_').replace('-', '_').toUpperCase();
        return String.format("Add to application.yml: %s: <value>\nOR set environment variable: export %s=<value>",
                propertyName, envVarName);
    }
    
    /**
     * Construit le message d'action pour l'utilisateur.
     */
    private String buildAction(List<ConfigurationError> errors) {
        StringBuilder action = new StringBuilder();
        action.append("Update your application's configuration\n\n");
        action.append("Fix the ").append(errors.size()).append(" error(s) above to start your application.\n");
        action.append("💡 TIP: Fix all errors at once to avoid multiple restarts!");
        return action.toString();
    }
    
    /**
     * Détecte le fichier source actif basé sur le profil Spring actif.
     */
    private PropertySource detectPropertySource() {
        // Essayer de détecter le profil actif depuis les propriétés système
        String activeProfiles = System.getProperty("spring.profiles.active");
        
        // Si pas dans les propriétés système, essayer les variables d'environnement
        if (activeProfiles == null || activeProfiles.isEmpty()) {
            activeProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        
        // Si un profil est actif, retourner le fichier correspondant
        if (activeProfiles != null && !activeProfiles.isEmpty()) {
            String profileName = activeProfiles.split(",")[0].trim(); // Prendre le premier profil
            String fileName = "application-" + profileName + ".yml";
            return new PropertySource(
                fileName,
                "classpath:" + fileName,
                PropertySource.SourceType.APPLICATION_YAML
            );
        }
        
        // Fallback: application.yml par défaut
        return new PropertySource(
            "application.yml",
            "classpath:application.yml",
            PropertySource.SourceType.APPLICATION_YAML
        );
    }
}
