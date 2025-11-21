package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de Sécurité & Reporting selon le cahier de recette.
 * Valide le formatage des erreurs et le masquage des secrets.
 */
@DisplayName("Spring Boot Security & Reporting - Cahier de Recette")
class SpringBootSecurityAndReportingTest {
    
    @Test
    @DisplayName("[TEST-SEC-01] Masquage de Secrets")
    void testSec01_MasquageDeSecrets() {
        // Given
        SecretDetector secretDetector = new SecretDetector();
        
        // When & Then - Propriétés sensibles
        assertThat(secretDetector.isSensitive("api.google.secret_key"))
            .as("api.google.secret_key doit être détectée comme sensible")
            .isTrue();
        
        assertThat(secretDetector.isSensitive("database.password"))
            .as("database.password doit être détectée comme sensible")
            .isTrue();
        
        assertThat(secretDetector.isSensitive("app.jwt.token"))
            .as("app.jwt.token doit être détectée comme sensible")
            .isTrue();
        
        assertThat(secretDetector.isSensitive("auth.api-key"))
            .as("auth.api-key doit être détectée comme sensible")
            .isTrue();
        
        // When & Then - Propriétés non sensibles
        assertThat(secretDetector.isSensitive("app.name"))
            .as("app.name ne doit pas être détectée comme sensible")
            .isFalse();
        
        assertThat(secretDetector.isSensitive("database.url"))
            .as("database.url ne doit pas être détectée comme sensible")
            .isFalse();
        
        // When & Then - Masquage
        String maskedValue = secretDetector.maskIfSensitive("api.secret", "my-secret-value-123");
        assertThat(maskedValue)
            .as("La valeur sensible doit être masquée")
            .isEqualTo("***MASKED***");
        
        String unmaskedValue = secretDetector.maskIfSensitive("app.name", "MyApp");
        assertThat(unmaskedValue)
            .as("La valeur non sensible ne doit pas être masquée")
            .isEqualTo("MyApp");
    }
    
    @Test
    @DisplayName("[TEST-REP-01] Agrégation d'Erreurs")
    void testRep01_AggregationDErreurs() {
        // Given - Simuler 3 propriétés manquantes
        MockEnvironment env = new MockEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        Map<String, Object> props = new HashMap<>();
        props.put("app.database.url", "${db.url}");       // Erreur 1
        props.put("app.database.username", "${db.user}"); // Erreur 2
        props.put("app.api.key", "${api.key}");          // Erreur 3
        propertySources.addFirst(new MapPropertySource("applicationConfig", props));
        
        SpringBootConfigurationValidator validator = new SpringBootConfigurationValidator(env);
        
        // When
        ValidationResult result = validator.validatePlaceholders();
        
        // Then
        assertThat(result.hasErrors())
            .as("Doit avoir des erreurs")
            .isTrue();
        
        List<ConfigurationError> errors = result.getErrors();
        assertThat(errors)
            .as("Doit contenir exactement 3 erreurs")
            .hasSize(3);
        
        // Vérifier que toutes les erreurs sont de type UNRESOLVED_PLACEHOLDER
        assertThat(errors)
            .as("Toutes les erreurs doivent être de type UNRESOLVED_PLACEHOLDER")
            .allMatch(error -> error.getType() == ErrorType.UNRESOLVED_PLACEHOLDER);
        
        // Vérifier que les 3 placeholders sont mentionnés
        List<String> errorMessages = errors.stream()
            .map(ConfigurationError::getErrorMessage)
            .toList();
        
        assertThat(errorMessages)
            .as("Les messages doivent mentionner db.url")
            .anyMatch(msg -> msg.contains("db.url"));
        assertThat(errorMessages)
            .as("Les messages doivent mentionner db.user")
            .anyMatch(msg -> msg.contains("db.user"));
        assertThat(errorMessages)
            .as("Les messages doivent mentionner api.key")
            .anyMatch(msg -> msg.contains("api.key"));
    }
    
    @Test
    @DisplayName("[TEST-REP-02] FailureAnalyzer (Spring Boot)")
    void testRep02_FailureAnalyzer() {
        // Given - Créer une exception avec des erreurs
        MockEnvironment env = new MockEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        Map<String, Object> props = new HashMap<>();
        props.put("app.database.url", "${db.url}");
        props.put("app.api.key", "${api.key}");
        propertySources.addFirst(new MapPropertySource("applicationConfig", props));
        
        SpringBootConfigurationValidator validator = new SpringBootConfigurationValidator(env);
        ValidationResult result = validator.validatePlaceholders();
        
        SpringBootValidatorEnvironmentPostProcessor.ConfigurationValidationException exception =
            new SpringBootValidatorEnvironmentPostProcessor.ConfigurationValidationException(
                "Configuration validation failed", result
            );
        
        ConfigurationValidationFailureAnalyzer analyzer = new ConfigurationValidationFailureAnalyzer();
        
        // When
        FailureAnalysis analysis = analyzer.analyze(exception, exception);
        
        // Then
        assertThat(analysis)
            .as("Le FailureAnalysis ne doit pas être null")
            .isNotNull();
        
        assertThat(analysis.getDescription())
            .as("La description doit contenir le tableau formaté")
            .isNotNull()
            .contains("CONFIGURATION VALIDATION FAILED")
            .contains("db.url")
            .contains("api.key");
        
        assertThat(analysis.getAction())
            .as("L'action doit contenir des instructions")
            .isNotNull()
            .contains("Fix the configuration errors")
            .contains("properties need to be configured");
        
        assertThat(analysis.getCause())
            .as("La cause doit être l'exception originale")
            .isEqualTo(exception);
    }
    
    @Test
    @DisplayName("[TEST-REP-EXTRA-01] Formatage avec Secrets")
    void testRepExtra01_FormattageAvecSecrets() {
        // Given
        ConfigurationError secretError = ConfigurationError.builder()
            .type(ErrorType.MISSING_PROPERTY)
            .propertyName("database.password")
            .errorMessage("Property 'database.password' is required")
            .suggestion("Set database.password")
            .isSensitive(true)
            .build();
        
        ConfigurationError normalError = ConfigurationError.builder()
            .type(ErrorType.MISSING_PROPERTY)
            .propertyName("app.name")
            .errorMessage("Property 'app.name' is required")
            .suggestion("Set app.name")
            .isSensitive(false)
            .build();
        
        ValidationResult result = new ValidationResult(List.of(secretError, normalError));
        BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
        
        // When
        String formatted = formatter.format(result);
        
        // Then
        assertThat(formatted)
            .as("Le formatage doit contenir le badge SENSITIVE")
            .contains("🔒 [SENSITIVE]")
            .contains("database.password");
        
        assertThat(formatted)
            .as("Le formatage doit contenir l'erreur normale sans badge")
            .contains("app.name")
            .doesNotContain("app.name 🔒");
    }
    
    @Test
    @DisplayName("[TEST-REP-EXTRA-02] Formatage Compact")
    void testRepExtra02_FormattageCompact() {
        // Given
        ConfigurationError error1 = ConfigurationError.builder()
            .type(ErrorType.MISSING_PROPERTY)
            .propertyName("app.url")
            .errorMessage("Property 'app.url' is required")
            .build();
        
        ConfigurationError error2 = ConfigurationError.builder()
            .type(ErrorType.UNRESOLVED_PLACEHOLDER)
            .propertyName("app.host")
            .errorMessage("Cannot resolve placeholder '${host}'")
            .build();
        
        ValidationResult result = new ValidationResult(List.of(error1, error2));
        BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
        
        // When
        String compact = formatter.formatCompact(result);
        
        // Then
        assertThat(compact)
            .as("Le format compact doit contenir le nombre d'erreurs")
            .contains("2 error(s)")
            .contains("app.url")
            .contains("app.host");
    }
    
    @Test
    @DisplayName("[TEST-REP-EXTRA-03] Groupement par Type")
    void testRepExtra03_GroupementParType() {
        // Given
        ConfigurationError error1 = ConfigurationError.builder()
            .type(ErrorType.MISSING_PROPERTY)
            .propertyName("app.url")
            .errorMessage("Missing")
            .build();
        
        ConfigurationError error2 = ConfigurationError.builder()
            .type(ErrorType.UNRESOLVED_PLACEHOLDER)
            .propertyName("app.host")
            .errorMessage("Unresolved")
            .build();
        
        ConfigurationError error3 = ConfigurationError.builder()
            .type(ErrorType.MISSING_PROPERTY)
            .propertyName("app.port")
            .errorMessage("Missing")
            .build();
        
        ValidationResult result = new ValidationResult(List.of(error1, error2, error3));
        
        // When
        Map<ErrorType, List<ConfigurationError>> grouped = result.getErrorsGroupedByType();
        
        // Then
        assertThat(grouped)
            .as("Doit contenir 2 types d'erreurs")
            .hasSize(2);
        
        assertThat(grouped.get(ErrorType.MISSING_PROPERTY))
            .as("Doit avoir 2 erreurs MISSING_PROPERTY")
            .hasSize(2);
        
        assertThat(grouped.get(ErrorType.UNRESOLVED_PLACEHOLDER))
            .as("Doit avoir 1 erreur UNRESOLVED_PLACEHOLDER")
            .hasSize(1);
    }
}
