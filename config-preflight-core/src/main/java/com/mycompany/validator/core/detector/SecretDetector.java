package com.mycompany.validator.core.detector;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Détecte et masque les propriétés sensibles (secrets, mots de passe, clés API).
 * Empêche l'affichage de valeurs sensibles dans les logs.
 */
public class SecretDetector {
    
    // Mots-clés indiquant une propriété sensible
    private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
        "password", "passwd", "pwd",
        "secret", "token", "key", "apikey", "api-key", "api_key",
        "credential", "auth", "authorization",
        "private", "certificate", "cert",
        "salt", "hash"
    );
    
    // Pattern pour détecter les propriétés sensibles
    private static final Pattern SENSITIVE_PATTERN = buildSensitivePattern();
    
    private static final String MASKED_VALUE = "***MASKED***";
    
    /**
     * Vérifie si un nom de propriété est sensible.
     * 
     * Exemples :
     * - database.password → true
     * - app.api-key → true
     * - app.database.url → false
     * 
     * @param propertyName Nom de la propriété
     * @return true si la propriété est sensible
     */
    public boolean isSensitive(String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return false;
        }
        
        String lowerName = propertyName.toLowerCase();
        return SENSITIVE_PATTERN.matcher(lowerName).find();
    }
    
    /**
     * Masque une valeur si la propriété est sensible.
     * 
     * @param propertyName Nom de la propriété
     * @param value Valeur à potentiellement masquer
     * @return Valeur masquée si sensible, sinon valeur originale
     */
    public String maskIfSensitive(String propertyName, String value) {
        if (value == null) {
            return null;
        }
        
        if (isSensitive(propertyName)) {
            return MASKED_VALUE;
        }
        
        return value;
    }
    
    /**
     * Masque partiellement une valeur (garde les premiers et derniers caractères).
     * Utile pour les identifiants qui ne sont pas totalement secrets.
     * 
     * Exemples :
     * - "my-secret-key-123" → "my***123"
     * - "short" → "s***t"
     * - "ab" → "***"
     * 
     * @param value Valeur à masquer
     * @return Valeur partiellement masquée
     */
    public String maskPartially(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        if (value.length() <= 2) {
            return "***";
        }
        
        if (value.length() <= 6) {
            return value.charAt(0) + "***" + value.charAt(value.length() - 1);
        }
        
        // Garder les 2 premiers et 3 derniers caractères
        return value.substring(0, 2) + "***" + value.substring(value.length() - 3);
    }
    
    /**
     * Génère un message d'erreur sécurisé qui ne révèle pas de valeurs sensibles.
     * 
     * @param propertyName Nom de la propriété
     * @param originalMessage Message original
     * @return Message sécurisé
     */
    public String sanitizeErrorMessage(String propertyName, String originalMessage) {
        if (originalMessage == null) {
            return null;
        }
        
        if (!isSensitive(propertyName)) {
            return originalMessage;
        }
        
        // Remplacer toute valeur potentielle par le masque
        return originalMessage.replaceAll("(?i)(value|vaut|equals?)\\s*['\"]?[^'\"\\s]+['\"]?", 
                                         "$1 " + MASKED_VALUE);
    }
    
    /**
     * Construit le pattern de détection des propriétés sensibles.
     */
    private static Pattern buildSensitivePattern() {
        StringBuilder patternBuilder = new StringBuilder();
        patternBuilder.append("(");
        
        for (int i = 0; i < SENSITIVE_KEYWORDS.size(); i++) {
            if (i > 0) {
                patternBuilder.append("|");
            }
            patternBuilder.append(Pattern.quote(SENSITIVE_KEYWORDS.get(i)));
        }
        
        patternBuilder.append(")");
        return Pattern.compile(patternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }
    
    /**
     * Ajoute des mots-clés personnalisés pour la détection de secrets.
     * Utile pour des propriétés spécifiques à votre application.
     * 
     * @param keywords Mots-clés supplémentaires
     */
    public void addSensitiveKeywords(String... keywords) {
        SENSITIVE_KEYWORDS.addAll(Arrays.asList(keywords));
    }
}
