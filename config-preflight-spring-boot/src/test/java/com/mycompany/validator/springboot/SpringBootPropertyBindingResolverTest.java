package com.mycompany.validator.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class SpringBootPropertyBindingResolverTest {
    
    @Test
    void testPropertyExistsViaEnvironmentVariable() {
        MockEnvironment env = new MockEnvironment();
        
        // Définir uniquement la variable d'environnement
        env.setProperty("APP_DATABASE_URL", "jdbc:postgresql://localhost:5432/mydb");
        
        SpringBootPropertyBindingResolver resolver = new SpringBootPropertyBindingResolver(env);
        
        // ✅ Doit trouver la propriété via la variable d'environnement
        assertTrue(resolver.propertyExists("app.database.url"));
        assertEquals("jdbc:postgresql://localhost:5432/mydb", 
            resolver.getPropertyValue("app.database.url"));
    }
    
    @Test
    void testPropertyExistsViaProperty() {
        MockEnvironment env = new MockEnvironment();
        
        // Définir uniquement la propriété
        env.setProperty("app.database.url", "jdbc:postgresql://localhost:5432/mydb");
        
        SpringBootPropertyBindingResolver resolver = new SpringBootPropertyBindingResolver(env);
        
        // ✅ Doit trouver la propriété directement
        assertTrue(resolver.propertyExists("app.database.url"));
        assertEquals("jdbc:postgresql://localhost:5432/mydb", 
            resolver.getPropertyValue("app.database.url"));
    }
    
    @Test
    void testPropertyDoesNotExist() {
        MockEnvironment env = new MockEnvironment();
        
        SpringBootPropertyBindingResolver resolver = new SpringBootPropertyBindingResolver(env);
        
        // ❌ La propriété n'existe sous aucune forme
        assertFalse(resolver.propertyExists("app.missing.property"));
        assertNull(resolver.getPropertyValue("app.missing.property"));
    }
    
    @Test
    void testFindActualPropertyName() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("APP_DATABASE_URL", "jdbc:postgresql://localhost:5432/mydb");
        
        SpringBootPropertyBindingResolver resolver = new SpringBootPropertyBindingResolver(env);
        
        // ✅ Doit retourner le nom sous lequel la propriété a été trouvée
        assertEquals("APP_DATABASE_URL", 
            resolver.findActualPropertyName("app.database.url"));
    }
    
    @Test
    void testGenerateSuggestion() {
        MockEnvironment env = new MockEnvironment();
        SpringBootPropertyBindingResolver resolver = new SpringBootPropertyBindingResolver(env);
        
        String suggestion = resolver.generateSuggestion("app.database.url");
        
        assertTrue(suggestion.contains("app.database.url"));
        assertTrue(suggestion.contains("APP_DATABASE_URL"));
    }
}
