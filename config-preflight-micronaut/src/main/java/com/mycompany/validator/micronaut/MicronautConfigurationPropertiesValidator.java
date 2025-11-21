package com.mycompany.validator.micronaut;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import com.mycompany.validator.core.model.PropertySource;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.event.BeanInitializingEvent;
import io.micronaut.context.event.BeanInitializedEventListener;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Validator qui intercepte tous les beans @ConfigurationProperties
 * après leur initialisation (property injection) et vérifie que leurs propriétés ne sont pas null.
 */
@Context
@Singleton
public class MicronautConfigurationPropertiesValidator implements BeanInitializedEventListener<Object>, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(MicronautConfigurationPropertiesValidator.class);
    
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    private final ConcurrentHashMap<Class<?>, Boolean> validatedClasses = new ConcurrentHashMap<>();
    private final List<ConfigurationError> allErrors = new ArrayList<>();
    private final AtomicBoolean hasReportedErrors = new AtomicBoolean(false);
    
    public MicronautConfigurationPropertiesValidator() {
        logger.info("🔍 MicronautConfigurationPropertiesValidator initialized");
    }
    
    @Override
    public Object onInitialized(BeanInitializingEvent<Object> event) {
        Object bean = event.getBean();
        Class<?> beanClass = bean.getClass();
        
        logger.debug("onInitialized called for bean: {}", beanClass.getSimpleName());
        
        // Ignorer les beans internes de Micronaut
        if (isInternalMicronautBean(beanClass)) {
            return bean;
        }
        
        // Vérifier si la classe a @ConfigurationProperties
        ConfigurationProperties annotation = findConfigurationPropertiesAnnotation(beanClass);
        if (annotation != null && !validatedClasses.containsKey(beanClass)) {
            validatedClasses.put(beanClass, true);
            String prefix = annotation.value();
            logger.info("Validating @ConfigurationProperties bean: {} with prefix: {}", beanClass.getSimpleName(), prefix);
            
            // Valider les propriétés de ce bean
            List<ConfigurationError> errors = validateBean(bean, prefix, beanClass);
            
            if (!errors.isEmpty()) {
                synchronized (allErrors) {
                    allErrors.addAll(errors);
                }
                
                // Rapporter les erreurs immédiatement
                if (hasReportedErrors.compareAndSet(false, true)) {
                    reportErrors();
                }
            }
        }
        
        return bean;
    }
    
    private void reportErrors() {
        if (!allErrors.isEmpty()) {
            ValidationResult result = new ValidationResult(allErrors);
            String formattedErrors = formatter.format(result);
            
            System.err.println(formattedErrors);
            logger.error("❌ Configuration validation failed with {} error(s)", allErrors.size());
            
            // Arrêter l'application
            throw new ConfigurationValidationException(
                "Configuration validation failed with " + allErrors.size() + " error(s)",
                result
            );
        }
    }
    
    /**
     * Vérifie si un bean fait partie des packages internes de Micronaut.
     */
    private boolean isInternalMicronautBean(Class<?> beanClass) {
        String packageName = beanClass.getPackage() != null ? beanClass.getPackage().getName() : "";
        return packageName.startsWith("io.micronaut.") 
            || packageName.startsWith("com.fasterxml.jackson.")
            || packageName.startsWith("io.netty.");
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
        // Convertir camelCase en kebab-case
        // maxConnections -> max-connections
        // apiKey -> api-key
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
    
    private String generateSuggestion(String propertyName) {
        String envVarName = propertyName.replace('.', '_').replace('-', '_').toUpperCase();
        return String.format("Add to application.yml: %s: <value>\nOR set environment variable: export %s=<value>",
                propertyName, envVarName);
    }
    
    @Override
    public int getOrder() {
        // S'exécuter en tout premier pour valider les beans dès leur création
        return Ordered.HIGHEST_PRECEDENCE;
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
