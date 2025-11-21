package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration pour le validateur Spring Boot.
 * S'active automatiquement au démarrage de l'application.
 */
@AutoConfiguration
@ConditionalOnProperty(
    name = "configuration.validator.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SpringBootValidatorAutoConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringBootValidatorAutoConfiguration.class);
    
    @Bean
    public SpringBootConfigurationValidator configurationValidator(Environment environment) {
        return new SpringBootConfigurationValidator(environment);
    }
    
    @Bean
    public BeautifulErrorFormatter errorFormatter() {
        return new BeautifulErrorFormatter();
    }
    
    @Bean
    public SpringBootRequiredPropertiesValidator requiredPropertiesValidator(Environment environment) {
        return new SpringBootRequiredPropertiesValidator(environment);
    }
    
    /**
     * Exception levée quand la validation échoue.
     */
    public static class ConfigurationValidationException extends RuntimeException {
        private final ValidationResult validationResult;
        
        public ConfigurationValidationException(String message, ValidationResult validationResult) {
            super(message);
            this.validationResult = validationResult;
        }
        
        public ValidationResult getValidationResult() {
            return validationResult;
        }
    }
}
