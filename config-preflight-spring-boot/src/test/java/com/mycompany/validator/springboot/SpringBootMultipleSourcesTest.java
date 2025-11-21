package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'Imports & Sources Multiples selon le cahier de recette.
 * Vérifie que le validateur scanne bien tout l'environnement chargé.
 */
@DisplayName("Spring Boot Multiple Sources - Cahier de Recette")
class SpringBootMultipleSourcesTest {
    
    @Test
    @DisplayName("[TEST-IMP-01] Import de Fichier")
    void testImp01_ImportDeFichier() {
        // Given - Simuler 2 PropertySources
        MockEnvironment env = new MockEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        // PropertySource 1: application.properties
        Map<String, Object> appProps = new HashMap<>();
        appProps.put("app.name", "MyApp");
        appProps.put("app.database.url", "${db.url}"); // Placeholder manquant
        propertySources.addFirst(new MapPropertySource("applicationConfig: [classpath:/application.properties]", appProps));
        
        // PropertySource 2: config/db.properties (importé)
        Map<String, Object> dbProps = new HashMap<>();
        dbProps.put("db.host", "${db.server}"); // Placeholder manquant dans le fichier importé
        propertySources.addLast(new MapPropertySource("Config resource 'file:config/db.properties'", dbProps));
        
        SpringBootConfigurationValidator validator = new SpringBootConfigurationValidator(env);
        
        // When
        ValidationResult result = validator.validatePlaceholders();
        
        // Then
        assertThat(result.hasErrors())
            .as("Doit détecter des erreurs")
            .isTrue();
        
        List<ConfigurationError> errors = result.getErrors();
        assertThat(errors)
            .as("Doit détecter 2 placeholders manquants")
            .hasSize(2);
        
        // Vérifier que les sources sont correctement identifiées
        ConfigurationError dbUrlError = errors.stream()
            .filter(e -> e.getPropertyName().equals("app.database.url"))
            .findFirst()
            .orElse(null);
        
        assertThat(dbUrlError)
            .as("Erreur sur app.database.url doit exister")
            .isNotNull();
        assertThat(dbUrlError.getSource())
            .as("La source doit être application.properties")
            .isNotNull();
        assertThat(dbUrlError.getSource().getName())
            .as("Le nom de la source doit contenir 'application.properties'")
            .contains("application.properties");
        
        ConfigurationError dbServerError = errors.stream()
            .filter(e -> e.getPropertyName().equals("db.host"))
            .findFirst()
            .orElse(null);
        
        assertThat(dbServerError)
            .as("Erreur sur db.host doit exister")
            .isNotNull();
        assertThat(dbServerError.getSource())
            .as("La source doit être db.properties")
            .isNotNull();
        assertThat(dbServerError.getSource().getName())
            .as("Le nom de la source doit contenir 'db.properties'")
            .contains("db.properties");
    }
    
    @Test
    @DisplayName("[TEST-IMP-02] Priorité des Sources")
    void testImp02_PrioritéDesSources() {
        // Given - Simuler plusieurs sources avec priorités
        MockEnvironment env = new MockEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        // PropertySource 1: application.properties (priorité basse)
        Map<String, Object> appProps = new HashMap<>();
        appProps.put("app.mode", "${app.mode.value}"); // Placeholder
        propertySources.addLast(new MapPropertySource("applicationConfig", appProps));
        
        // PropertySource 2: Environment Variables (priorité haute)
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("app.mode.value", "production"); // Résout le placeholder
        propertySources.addFirst(new MapPropertySource("systemEnvironment", envVars));
        
        SpringBootConfigurationValidator validator = new SpringBootConfigurationValidator(env);
        
        // When
        ValidationResult result = validator.validatePlaceholders();
        
        // Then
        assertThat(result.isValid())
            .as("Doit être valide car la variable d'env résout le placeholder")
            .isTrue();
        assertThat(result.getErrors())
            .as("Ne doit pas avoir d'erreurs")
            .isEmpty();
    }
    
    @Test
    @DisplayName("[TEST-IMP-EXTRA-01] Sources Multiples avec Résolution Partielle")
    void testImpExtra01_SourcesMultiplesAvecResolutionPartielle() {
        // Given
        MockEnvironment env = new MockEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        // Source 1: Définit certaines propriétés
        Map<String, Object> source1 = new HashMap<>();
        source1.put("app.url", "${app.protocol}://${app.host}:${app.port}");
        source1.put("app.protocol", "https"); // Résolu
        propertySources.addFirst(new MapPropertySource("source1", source1));
        
        // Source 2: Définit d'autres propriétés
        Map<String, Object> source2 = new HashMap<>();
        source2.put("app.host", "localhost"); // Résolu
        // app.port manque
        propertySources.addLast(new MapPropertySource("source2", source2));
        
        SpringBootConfigurationValidator validator = new SpringBootConfigurationValidator(env);
        
        // When
        ValidationResult result = validator.validatePlaceholders();
        
        // Then
        assertThat(result.hasErrors())
            .as("Doit détecter une erreur")
            .isTrue();
        
        List<ConfigurationError> errors = result.getErrors();
        assertThat(errors)
            .as("Doit détecter uniquement app.port manquant")
            .hasSize(1);
        
        ConfigurationError error = errors.get(0);
        assertThat(error.getErrorMessage())
            .as("Le message doit mentionner app.port")
            .contains("app.port");
    }
    
    @Test
    @DisplayName("[TEST-IMP-EXTRA-02] Toutes les Sources Valides")
    void testImpExtra02_ToutesLesSourcesValides() {
        // Given
        MockEnvironment env = new MockEnvironment();
        MutablePropertySources propertySources = env.getPropertySources();
        
        // Source 1
        Map<String, Object> source1 = new HashMap<>();
        source1.put("app.name", "MyApp");
        source1.put("app.version", "1.0.0");
        propertySources.addFirst(new MapPropertySource("source1", source1));
        
        // Source 2
        Map<String, Object> source2 = new HashMap<>();
        source2.put("app.env", "production");
        propertySources.addLast(new MapPropertySource("source2", source2));
        
        SpringBootConfigurationValidator validator = new SpringBootConfigurationValidator(env);
        
        // When
        ValidationResult result = validator.validateAll();
        
        // Then
        assertThat(result.isValid())
            .as("Doit être valide")
            .isTrue();
        assertThat(result.getErrors())
            .as("Ne doit pas avoir d'erreurs")
            .isEmpty();
    }
}
