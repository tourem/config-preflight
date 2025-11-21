package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

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
    public SpringBootConfigurationPropertiesValidator configPreflightPropertiesValidator(
            org.springframework.context.ApplicationContext applicationContext) {
        return new SpringBootConfigurationPropertiesValidator(applicationContext);
    }
    
    @Bean
    public ApplicationListener<ApplicationReadyEvent> validationListener(
            SpringBootConfigurationValidator validator,
            SpringBootConfigurationPropertiesValidator configPreflightPropertiesValidator,
            BeautifulErrorFormatter formatter) {
        
        return event -> {
            logger.info("Running configuration validation...");
            
            // Valider les placeholders
            ValidationResult placeholderResult = validator.validatePlaceholders();
            
            // Valider les @ConfigurationProperties beans
            ValidationResult propertiesResult = configPreflightPropertiesValidator.validate();
            
            // Combiner les résultats
            List<com.mycompany.validator.core.model.ConfigurationError> allErrors = new ArrayList<>();
            allErrors.addAll(placeholderResult.getErrors());
            allErrors.addAll(propertiesResult.getErrors());
            
            ValidationResult combinedResult = new ValidationResult(allErrors);
            
            if (combinedResult.hasErrors()) {
                String formattedErrors = formatter.format(combinedResult);
                System.err.println(formattedErrors);
                logger.error("❌ Configuration validation failed with {} error(s)", combinedResult.getErrorCount());
                
                // Arrêter l'application si des erreurs sont trouvées
                throw new ConfigurationValidationException(
                    "Configuration validation failed with " + combinedResult.getErrorCount() + " error(s)",
                    combinedResult
                );
            } else {
                logger.info("✅ Configuration validation passed!");
            }
        };
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
