package com.mycompany.validator.core.detector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests avancés du PlaceholderDetector selon le cahier de recette.
 * Valide la logique regex pure, indépendamment du framework.
 */
@DisplayName("PlaceholderDetector - Cahier de Recette")
class PlaceholderDetectorAdvancedTest {
    
    private final PlaceholderDetector detector = new PlaceholderDetector();
    
    @Test
    @DisplayName("[TEST-CORE-01] Placeholder Manquant")
    void testCore01_PlaceholderManquant() {
        // Given
        String value = "Bonjour ${nom}";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Doit détecter le placeholder 'nom'")
            .hasSize(1)
            .containsExactly("nom");
    }
    
    @Test
    @DisplayName("[TEST-CORE-02] Placeholder avec Valeur par Défaut (Feature v3)")
    void testCore02_PlaceholderAvecValeurParDefaut() {
        // Given
        String value = "Hôte: ${server.host:localhost}";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Ne doit pas détecter d'erreur car valeur par défaut présente")
            .isEmpty();
        
        // Vérifier que detectPlaceholders détecte quand même le placeholder
        List<String> allPlaceholders = detector.detectPlaceholders(value);
        assertThat(allPlaceholders)
            .as("detectPlaceholders doit trouver le placeholder")
            .hasSize(1)
            .containsExactly("server.host");
    }
    
    @Test
    @DisplayName("[TEST-CORE-03] Placeholders Multiples")
    void testCore03_PlaceholdersMultiples() {
        // Given
        String value = "JDBC: ${db.url} | User: ${db.user}";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Doit détecter les deux placeholders manquants")
            .hasSize(2)
            .containsExactlyInAnyOrder("db.url", "db.user");
    }
    
    @Test
    @DisplayName("[TEST-CORE-04] Placeholder Imbriqué (Optionnel)")
    void testCore04_PlaceholderImbrique() {
        // Given
        String value = "${app.url:${base.url:http://localhost}}";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Aucune erreur car valeur par défaut présente (cas complexe)")
            .isEmpty();
        
        // Vérifier que tous les placeholders sont détectés
        List<String> allPlaceholders = detector.detectPlaceholders(value);
        assertThat(allPlaceholders)
            .as("Doit détecter les placeholders imbriqués")
            .hasSizeGreaterThanOrEqualTo(1);
    }
    
    @Test
    @DisplayName("[TEST-CORE-EXTRA-01] Mix de Placeholders avec et sans Défaut")
    void testCoreExtra01_MixPlaceholders() {
        // Given
        String value = "URL: ${app.url} | Port: ${app.port:8080} | Host: ${app.host}";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Doit détecter uniquement les placeholders SANS valeur par défaut")
            .hasSize(2)
            .containsExactlyInAnyOrder("app.url", "app.host")
            .doesNotContain("app.port");
    }
    
    @Test
    @DisplayName("[TEST-CORE-EXTRA-02] Placeholder Vide")
    void testCoreExtra02_PlaceholderVide() {
        // Given
        String value = "Test: ${}";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Ne doit pas détecter de placeholder vide")
            .isEmpty();
    }
    
    @Test
    @DisplayName("[TEST-CORE-EXTRA-03] Valeur par Défaut Vide")
    void testCoreExtra03_ValeurParDefautVide() {
        // Given
        String value = "Test: ${app.value:}";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Ne doit pas détecter d'erreur même si valeur par défaut vide")
            .isEmpty();
    }
    
    @Test
    @DisplayName("[TEST-CORE-EXTRA-04] Placeholder avec Espaces")
    void testCoreExtra04_PlaceholderAvecEspaces() {
        // Given
        String value = "Test: ${ app.name } et ${ app.port : 8080 }";
        
        // When
        List<String> requiredPlaceholders = detector.detectRequiredPlaceholders(value);
        
        // Then
        assertThat(requiredPlaceholders)
            .as("Doit gérer les espaces et détecter app.name")
            .hasSize(1)
            .containsExactly("app.name");
    }
}
