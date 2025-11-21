package com.mycompany.validator.micronaut;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import com.mycompany.validator.core.model.PropertySource;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Validator qui scanne tous les beans @ConfigurationProperties
 * et vérifie que leurs propriétés ne sont pas null.
 */
@Singleton
public class MicronautConfigurationPropertiesValidator implements ApplicationEventListener<ServerStartupEvent>, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(MicronautConfigurationPropertiesValidator.class);
    
    private final BeanContext beanContext;
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    public MicronautConfigurationPropertiesValidator(BeanContext beanContext) {
        this.beanContext = beanContext;
    }
    
    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        logger.info("🔍 Scanning @ConfigurationProperties beans for null values...");
        
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Trouver tous les bean definitions avec @ConfigurationProperties
        Collection<?> beanDefinitions = beanContext.getBeanDefinitions(Object.class);
        
        for (Object beanDef : beanDefinitions) {
            if (beanDef instanceof io.micronaut.inject.BeanDefinition) {
                io.micronaut.inject.BeanDefinition<?> definition = (io.micronaut.inject.BeanDefinition<?>) beanDef;
                Class<?> beanClass = definition.getBeanType();
                
                // Vérifier si la classe a @ConfigurationProperties
                ConfigurationProperties annotation = findConfigurationPropertiesAnnotation(beanClass);
                if (annotation != null) {
                    String prefix = annotation.value();
                    logger.debug("Found @ConfigurationProperties bean: {} with prefix: {}", beanClass.getSimpleName(), prefix);
                    
                    // Récupérer le bean
                    try {
                        Object bean = beanContext.getBean(beanClass);
                        // Valider les propriétés de ce bean
                        errors.addAll(validateBean(bean, prefix, beanClass));
                    } catch (Exception e) {
                        logger.warn("Could not get bean for class {}: {}", beanClass.getSimpleName(), e.getMessage());
                    }
                }
            }
        }
        
        if (!errors.isEmpty()) {
            ValidationResult result = new ValidationResult(errors);
            String formattedErrors = formatter.format(result);
            
            System.err.println(formattedErrors);
            logger.error("❌ Configuration validation failed with {} error(s)", errors.size());
            
            // Arrêter l'application
            throw new ConfigurationValidationException(
                "Configuration validation failed with " + errors.size() + " error(s)",
                result
            );
        } else {
            logger.info("✅ All @ConfigurationProperties beans are properly configured");
        }
    }
    
    private ConfigurationProperties findConfigurationPropertiesAnnotation(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            ConfigurationProperties annotation = current.getAnnotation(ConfigurationProperties.class);
            if (annotation != null) {
                return annotation;
            }
            current = current.getSuperclass();
        }
        return null;
    }
    
    private List<ConfigurationError> validateBean(Object bean, String prefix, Class<?> beanClass) {
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Parcourir tous les champs
        for (Field field : beanClass.getDeclaredFields()) {
            field.setAccessible(true);
            
            try {
                Object value = field.get(bean);
                String fieldName = field.getName();
                String fullPropertyName = prefix.isEmpty() ? fieldName : prefix + "." + convertToKebabCase(fieldName);
                
                // Vérifier si la valeur est null
                if (value == null) {
                    boolean isSensitive = secretDetector.isSensitive(fullPropertyName);
                    
                    errors.add(ConfigurationError.builder()
                            .type(ErrorType.MISSING_PROPERTY)
                            .propertyName(fullPropertyName)
                            .errorMessage(String.format("Property '%s' is not set", fullPropertyName))
                            .suggestion(generateSuggestion(fullPropertyName))
                            .source(new PropertySource(
                                    "application.yml",
                                    "classpath:/application.yml",
                                    PropertySource.SourceType.APPLICATION_YAML
                            ))
                            .isSensitive(isSensitive)
                            .build());
                    
                    logger.warn("Property '{}' is null in bean {}", fullPropertyName, beanClass.getSimpleName());
                }
            } catch (IllegalAccessException e) {
                logger.warn("Cannot access field {} in {}", field.getName(), beanClass.getSimpleName());
            }
        }
        
        return errors;
    }
    
    private String convertToKebabCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
    
    private String generateSuggestion(String propertyName) {
        String envVarName = propertyName.replace('.', '_').replace('-', '_').toUpperCase();
        return String.format("Add to application.yml: %s: <value>\nOR set environment variable: export %s=<value>",
                propertyName, envVarName);
    }
    
    @Override
    public int getOrder() {
        // S'exécuter après MicronautEarlyValidator mais avant le démarrage du serveur
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
    
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
