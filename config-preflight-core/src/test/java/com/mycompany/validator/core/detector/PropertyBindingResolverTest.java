package com.mycompany.validator.core.detector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PropertyBindingResolverTest {
    
    private final PropertyBindingResolver resolver = new PropertyBindingResolver();
    
    @Test
    void testToEnvironmentVariableName() {
        assertEquals("APP_DATABASE_URL", 
            resolver.toEnvironmentVariableName("app.database.url"));
        
        assertEquals("MY_APP_REDIS_HOST", 
            resolver.toEnvironmentVariableName("my-app.redis-host"));
        
        assertEquals("SPRING_DATASOURCE_URL", 
            resolver.toEnvironmentVariableName("spring.datasource.url"));
    }
    
    @Test
    void testToPropertyName() {
        assertEquals("app.database.url", 
            resolver.toPropertyName("APP_DATABASE_URL"));
        
        assertEquals("my.app.redis.host", 
            resolver.toPropertyName("MY_APP_REDIS_HOST"));
    }
    
    @Test
    void testGetAllVariants() {
        List<String> variants = resolver.getAllVariants("app.database.url");
        
        assertTrue(variants.contains("app.database.url"));          // Original
        assertTrue(variants.contains("APP_DATABASE_URL"));          // Env var uppercase
        assertTrue(variants.contains("app_database_url"));          // Env var lowercase
        assertTrue(variants.contains("appDatabaseUrl"));            // CamelCase
        assertTrue(variants.contains("app-database-url"));          // Kebab-case
    }
    
    @Test
    void testLooksLikeEnvironmentVariable() {
        assertTrue(resolver.looksLikeEnvironmentVariable("DATABASE_URL"));
        assertTrue(resolver.looksLikeEnvironmentVariable("APP_DATABASE_PASSWORD"));
        
        assertFalse(resolver.looksLikeEnvironmentVariable("app.database.url"));
        assertFalse(resolver.looksLikeEnvironmentVariable("appDatabaseUrl"));
    }
    
    @Test
    void testLooksLikeProperty() {
        assertTrue(resolver.looksLikeProperty("app.database.url"));
        assertTrue(resolver.looksLikeProperty("my-app.redis-host"));
        
        assertFalse(resolver.looksLikeProperty("DATABASE_URL"));
        assertFalse(resolver.looksLikeProperty("APP_DATABASE_PASSWORD"));
    }
}
