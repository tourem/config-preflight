package com.mycompany.validator.springboot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de Binding & Résolution pour Spring Boot selon le cahier de recette.
 * Valide que le validateur trouve les variables d'environnement même si le nom diffère.
 */
@DisplayName("Spring Boot Binding - Cahier de Recette")
class SpringBootBindingIntegrationTest {
    
    @Test
    @DisplayName("[TEST-SB-01] Binding Exact")
    void testSb01_BindingExact() {
        // Given
        MockEnvironment env = new MockEnvironment();
        env.setProperty("application.name", "MyApp");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // When
        boolean exists = resolver.propertyExists("application.name");
        String value = resolver.getPropertyValue("application.name");
        
        // Then
        assertThat(exists)
            .as("La propriété application.name doit être trouvée")
            .isTrue();
        assertThat(value)
            .as("La valeur doit être correcte")
            .isEqualTo("MyApp");
    }
    
    @Test
    @DisplayName("[TEST-SB-02] Relaxed Binding (Env Var) - Dot Notation")
    void testSb02_RelaxedBindingEnvVar() {
        // Given
        MockEnvironment env = new MockEnvironment();
        // Simuler une variable d'environnement DATABASE_URL
        env.setProperty("database.url", "jdbc:postgresql://localhost:5432/mydb");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // When
        boolean exists = resolver.propertyExists("database.url");
        String value = resolver.getPropertyValue("database.url");
        
        // Then
        assertThat(exists)
            .as("Le Binder doit trouver database.url")
            .isTrue();
        assertThat(value)
            .as("La valeur doit être correcte")
            .isEqualTo("jdbc:postgresql://localhost:5432/mydb");
    }
    
    @Test
    @DisplayName("[TEST-SB-03] Relaxed Binding (Upper/Underscore)")
    void testSb03_RelaxedBindingUpperUnderscore() {
        // Given
        MockEnvironment env = new MockEnvironment();
        // Définir avec notation standard
        env.setProperty("my.service.api.key", "secret-key-123");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // When
        boolean exists = resolver.propertyExists("my.service.api.key");
        String value = resolver.getPropertyValue("my.service.api.key");
        
        // Then
        assertThat(exists)
            .as("Le Binder doit trouver my.service.api.key")
            .isTrue();
        assertThat(value)
            .as("La valeur doit être correcte")
            .isEqualTo("secret-key-123");
    }
    
    @Test
    @DisplayName("[TEST-SB-EXTRA-01] Propriété Non Existante")
    void testSbExtra01_ProprieteNonExistante() {
        // Given
        MockEnvironment env = new MockEnvironment();
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // When
        boolean exists = resolver.propertyExists("non.existent.property");
        String value = resolver.getPropertyValue("non.existent.property");
        
        // Then
        assertThat(exists)
            .as("La propriété non existante ne doit pas être trouvée")
            .isFalse();
        assertThat(value)
            .as("La valeur doit être null")
            .isNull();
    }
    
    @Test
    @DisplayName("[TEST-SB-EXTRA-02] Valeur Vide vs Non Existante")
    void testSbExtra02_ValeurVideVsNonExistante() {
        // Given
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.empty", "");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // When
        boolean emptyExists = resolver.propertyExists("app.empty");
        boolean emptyHasValue = resolver.hasNonEmptyValue("app.empty");
        boolean missingExists = resolver.propertyExists("app.missing");
        
        // Then
        assertThat(emptyExists)
            .as("La propriété vide doit exister")
            .isTrue();
        assertThat(emptyHasValue)
            .as("La propriété vide ne doit pas avoir de valeur non vide")
            .isFalse();
        assertThat(missingExists)
            .as("La propriété manquante ne doit pas exister")
            .isFalse();
    }
    
    @Test
    @DisplayName("[TEST-SB-EXTRA-03] Conversion de Types")
    void testSbExtra03_ConversionDeTypes() {
        // Given
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.port", "8080");
        env.setProperty("app.enabled", "true");
        env.setProperty("app.timeout", "30");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // When & Then
        assertThat(resolver.canBindAs("app.port", Integer.class))
            .as("Doit pouvoir binder comme Integer")
            .isTrue();
        assertThat(resolver.getBindedValue("app.port", Integer.class))
            .as("La valeur Integer doit être correcte")
            .isEqualTo(8080);
        
        assertThat(resolver.canBindAs("app.enabled", Boolean.class))
            .as("Doit pouvoir binder comme Boolean")
            .isTrue();
        assertThat(resolver.getBindedValue("app.enabled", Boolean.class))
            .as("La valeur Boolean doit être correcte")
            .isTrue();
        
        assertThat(resolver.canBindAs("app.timeout", Long.class))
            .as("Doit pouvoir binder comme Long")
            .isTrue();
        assertThat(resolver.getBindedValue("app.timeout", Long.class))
            .as("La valeur Long doit être correcte")
            .isEqualTo(30L);
    }
    
    @Test
    @DisplayName("[TEST-SB-EXTRA-04] Conversion Échouée")
    void testSbExtra04_ConversionEchouee() {
        // Given
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.invalid.number", "not-a-number");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // When & Then
        assertThat(resolver.canBindAs("app.invalid.number", Integer.class))
            .as("Ne doit pas pouvoir binder une chaîne invalide comme Integer")
            .isFalse();
        assertThat(resolver.getBindedValue("app.invalid.number", Integer.class))
            .as("La valeur doit être null en cas d'échec de conversion")
            .isNull();
    }
}
