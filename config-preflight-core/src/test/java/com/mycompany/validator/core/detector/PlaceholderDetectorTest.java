package com.mycompany.validator.core.detector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlaceholderDetectorTest {
    
    private final PlaceholderDetector detector = new PlaceholderDetector();
    
    @Test
    void testDetectPlaceholders() {
        List<String> placeholders = detector.detectPlaceholders("jdbc:postgresql://${db.host}:${db.port}/mydb");
        
        assertEquals(2, placeholders.size());
        assertTrue(placeholders.contains("db.host"));
        assertTrue(placeholders.contains("db.port"));
    }
    
    @Test
    void testDetectPlaceholdersWithDefaultValue() {
        List<String> placeholders = detector.detectPlaceholders("${db.url:jdbc:h2:mem:testdb}");
        
        assertEquals(1, placeholders.size());
        assertEquals("db.url", placeholders.get(0));
    }
    
    @Test
    void testHasPlaceholders() {
        assertTrue(detector.hasPlaceholders("${db.host}"));
        assertTrue(detector.hasPlaceholders("Server: ${server.name}"));
        
        assertFalse(detector.hasPlaceholders("no placeholders here"));
        assertFalse(detector.hasPlaceholders(""));
        assertFalse(detector.hasPlaceholders(null));
    }
    
    @Test
    void testExtractDefaultValue() {
        assertEquals("jdbc:h2:mem:testdb", 
            detector.extractDefaultValue("${db.url:jdbc:h2:mem:testdb}"));
        
        assertNull(detector.extractDefaultValue("${db.url}"));
    }
    
    @Test
    void testHasDefaultValue() {
        assertTrue(detector.hasDefaultValue("${db.url:jdbc:h2:mem:testdb}"));
        assertFalse(detector.hasDefaultValue("${db.url}"));
    }
    
    @Test
    void testDetectRequiredPlaceholders() {
        // Placeholder sans valeur par défaut → requis
        List<String> required = detector.detectRequiredPlaceholders("${DATABASE_URL}");
        assertEquals(1, required.size());
        assertEquals("DATABASE_URL", required.get(0));
        
        // Placeholder avec valeur par défaut → non requis
        required = detector.detectRequiredPlaceholders("${DATABASE_URL:jdbc:h2:mem}");
        assertEquals(0, required.size());
        
        // Mix des deux
        required = detector.detectRequiredPlaceholders("jdbc:postgresql://${DB_HOST}:${DB_PORT:5432}/mydb");
        assertEquals(1, required.size());
        assertEquals("DB_HOST", required.get(0)); // DB_PORT a une valeur par défaut
    }
}
