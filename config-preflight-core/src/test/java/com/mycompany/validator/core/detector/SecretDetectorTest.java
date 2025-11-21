package com.mycompany.validator.core.detector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretDetectorTest {
    
    private final SecretDetector detector = new SecretDetector();
    
    @Test
    void testIsSensitive() {
        // Propriétés sensibles
        assertTrue(detector.isSensitive("database.password"));
        assertTrue(detector.isSensitive("app.api-key"));
        assertTrue(detector.isSensitive("app.apiKey"));
        assertTrue(detector.isSensitive("jwt.secret"));
        assertTrue(detector.isSensitive("auth.token"));
        assertTrue(detector.isSensitive("private.key"));
        
        // Propriétés non sensibles
        assertFalse(detector.isSensitive("database.url"));
        assertFalse(detector.isSensitive("app.name"));
        assertFalse(detector.isSensitive("server.port"));
    }
    
    @Test
    void testMaskIfSensitive() {
        // Propriété sensible → masquée
        assertEquals("***MASKED***", 
            detector.maskIfSensitive("database.password", "secret123"));
        
        // Propriété non sensible → non masquée
        assertEquals("jdbc:postgresql://localhost:5432/mydb", 
            detector.maskIfSensitive("database.url", "jdbc:postgresql://localhost:5432/mydb"));
    }
    
    @Test
    void testMaskPartially() {
        assertEquals("my***123", detector.maskPartially("my-secret-key-123"));
        assertEquals("s***t", detector.maskPartially("short"));
        assertEquals("***", detector.maskPartially("ab"));
        assertEquals("", detector.maskPartially(""));
    }
    
    @Test
    void testSanitizeErrorMessage() {
        String propertyName = "database.password";
        String message = "Property 'database.password' has value 'secret123' but is empty";
        
        String sanitized = detector.sanitizeErrorMessage(propertyName, message);
        
        // La valeur doit être masquée
        assertTrue(sanitized.contains("***MASKED***"));
        assertFalse(sanitized.contains("secret123"));
    }
}
