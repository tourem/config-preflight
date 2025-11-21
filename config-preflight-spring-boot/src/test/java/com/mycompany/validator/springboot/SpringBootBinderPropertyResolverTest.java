package com.mycompany.validator.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class SpringBootBinderPropertyResolverTest {
    
    @Test
    void testPropertyExistsWithBinder() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.database.url", "jdbc:postgresql://localhost:5432/mydb");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // Le Binder doit trouver la propriété
        assertTrue(resolver.propertyExists("app.database.url"));
        assertEquals("jdbc:postgresql://localhost:5432/mydb", 
            resolver.getPropertyValue("app.database.url"));
    }
    
    @Test
    void testPropertyDoesNotExist() {
        MockEnvironment env = new MockEnvironment();
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        assertFalse(resolver.propertyExists("app.missing.property"));
        assertNull(resolver.getPropertyValue("app.missing.property"));
    }
    
    @Test
    void testHasNonEmptyValue() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.name", "MyApp");
        env.setProperty("app.empty", "");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        assertTrue(resolver.hasNonEmptyValue("app.name"));
        assertFalse(resolver.hasNonEmptyValue("app.empty"));
        assertFalse(resolver.hasNonEmptyValue("app.missing"));
    }
    
    @Test
    void testCanBindAs() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("app.port", "8080");
        env.setProperty("app.enabled", "true");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // Peut binder comme Integer
        assertTrue(resolver.canBindAs("app.port", Integer.class));
        assertEquals(8080, resolver.getBindedValue("app.port", Integer.class));
        
        // Peut binder comme Boolean
        assertTrue(resolver.canBindAs("app.enabled", Boolean.class));
        assertEquals(true, resolver.getBindedValue("app.enabled", Boolean.class));
    }
    
    @Test
    void testRelaxedBindingWithDotNotation() {
        MockEnvironment env = new MockEnvironment();
        // Définir avec dot notation
        env.setProperty("app.database.url", "jdbc:h2:mem:testdb");
        
        SpringBootBinderPropertyResolver resolver = new SpringBootBinderPropertyResolver(env);
        
        // Le Binder doit trouver la propriété
        assertTrue(resolver.propertyExists("app.database.url"));
        assertEquals("jdbc:h2:mem:testdb", resolver.getPropertyValue("app.database.url"));
    }
}
