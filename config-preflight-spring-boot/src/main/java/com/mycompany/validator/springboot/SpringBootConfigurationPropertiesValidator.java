package com.mycompany.validator.springboot;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.detector.SecretDetector;
import com.mycompany.validator.core.formatter.BeautifulErrorFormatter;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import com.mycompany.validator.core.model.PropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validator qui scanne automatiquement tous les beans @ConfigurationProperties
 * et vérifie que leurs propriétés requises ne sont pas null.
 * S'exécute après la création du contexte mais AVANT ApplicationReadyEvent.
 */
public class SpringBootConfigurationPropertiesValidator implements ApplicationListener<ApplicationStartedEvent>, Ordered {
    
    private static final Logger logger = LoggerFactory.getLogger(SpringBootConfigurationPropertiesValidator.class);
    
    private final ApplicationContext applicationContext;
    private final SecretDetector secretDetector = new SecretDetector();
    private final BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
    
    public SpringBootConfigurationPropertiesValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
    
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        logger.info("🔍 Scanning @ConfigurationProperties beans for null values...");
        
        List<ConfigurationError> errors = new ArrayList<>();
        
        // Récupérer tous les beans avec @ConfigurationProperties
        Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
        
        for (Map.Entry<String, Object> entry : configBeans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            
            // Ignorer les beans internes de Spring
            if (isInternalSpringBean(beanClass)) {
                continue;
            }
            
            ConfigurationProperties annotation = findConfigurationPropertiesAnnotation(beanClass);
            if (annotation != null) {
                String prefix = annotation.value().isEmpty() ? annotation.prefix() : annotation.value();
                logger.debug("Found @ConfigurationProperties bean: {} with prefix: {}", beanClass.getSimpleName(), prefix);
                
                // Valider les propriétés de ce bean
                errors.addAll(validateBean(bean, prefix, beanClass));
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
    
    private boolean isInternalSpringBean(Class<?> beanClass) {
        String packageName = beanClass.getPackage() != null ? beanClass.getPackage().getName() : "";
        return packageName.startsWith("org.springframework.") 
            || packageName.startsWith("org.apache.")
            || packageName.startsWith("com.fasterxml.jackson.");
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
        
        // Parcourir tous les champs déclarés
        for (Field field : beanClass.getDeclaredFields()) {
            field.setAccessible(true);
            
            try {
                Object value = field.get(bean);
                
                // Si la valeur est null, c'est une erreur
                if (value == null) {
                    String fieldName = field.getName();
                    String kebabCaseName = convertToKebabCase(fieldName);
                    String propertyName = prefix.isEmpty() ? kebabCaseName : prefix + "." + kebabCaseName;
                    
                    boolean isSensitive = secretDetector.isSensitive(propertyName);
                    
                    logger.warn("Property '{}' is null in bean {}", propertyName, beanClass.getSimpleName());
                    
                    // Détecter la source réelle de la propriété
                    PropertySource source = detectPropertySource(propertyName);
                    
                    errors.add(ConfigurationError.builder()
                        .type(ErrorType.MISSING_PROPERTY)
                        .propertyName(propertyName)
                        .source(source)
                        .errorMessage("Property '" + propertyName + "' is not set")
                        .suggestion(generateSuggestion(propertyName))
                        .isSensitive(isSensitive)
                        .build());
                }
            } catch (IllegalAccessException e) {
                logger.warn("Cannot access field {} in {}", field.getName(), beanClass.getSimpleName());
            }
        }
        
        return errors;
    }
    
    private String convertToKebabCase(String camelCase) {
        return camelCase.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
    }
    
    private String generateSuggestion(String propertyName) {
        String envVarName = propertyName.replace('.', '_').replace('-', '_').toUpperCase();
        return String.format("Add to application.yml: %s: <value>\nOR set environment variable: export %s=<value>",
                propertyName, envVarName);
    }
    
    /**
     * Détecte le fichier source d'où devrait provenir une propriété.
     * Vérifie dans quel fichier les autres propriétés du même prefix sont définies.
     */
    private PropertySource detectPropertySource(String propertyName) {
        org.springframework.core.env.Environment environment = applicationContext.getEnvironment();
        if (!(environment instanceof ConfigurableEnvironment)) {
            return new PropertySource("application.yml", "classpath:application.yml", PropertySource.SourceType.APPLICATION_YAML);
        }
        ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
        
        // Extraire le prefix (ex: "database" de "database.max-connections")
        String prefix = propertyName.contains(".") ? propertyName.substring(0, propertyName.indexOf(".")) : propertyName;
        
        // Chercher dans les PropertySources pour trouver où ce prefix est défini
        String profileSpecificFile = null;
        String defaultFile = null;
        
        for (org.springframework.core.env.PropertySource<?> ps : env.getPropertySources()) {
            String sourceName = ps.getName();
            
            if (sourceName.contains("application") && 
                (sourceName.contains(".yml") || sourceName.contains(".yaml") || sourceName.contains(".properties"))) {
                
                String fileName = extractFileName(sourceName);
                
                // Vérifier si ce PropertySource contient des propriétés avec notre prefix
                if (ps instanceof EnumerablePropertySource) {
                    EnumerablePropertySource<?> enumPs = (EnumerablePropertySource<?>) ps;
                    for (String propName : enumPs.getPropertyNames()) {
                        if (propName.startsWith(prefix + ".") || propName.startsWith(prefix + "[")) {
                            // Cette source contient des propriétés de notre prefix
                            if (fileName.contains("-") && !fileName.equals("application.yml")) {
                                profileSpecificFile = fileName;
                                break;
                            } else if (fileName.equals("application.yml") || fileName.equals("application.properties")) {
                                defaultFile = fileName;
                            }
                        }
                    }
                    
                    if (profileSpecificFile != null) {
                        break; // On a trouvé le fichier spécifique au profil
                    }
                }
            }
        }
        
        // Retourner le fichier où le prefix est défini
        String selectedFile = profileSpecificFile != null ? profileSpecificFile : 
                             (defaultFile != null ? defaultFile : "application.yml");
        
        return new PropertySource(
            selectedFile,
            "classpath:" + selectedFile,
            selectedFile.endsWith(".properties") ? 
                PropertySource.SourceType.APPLICATION_PROPERTIES : 
                PropertySource.SourceType.APPLICATION_YAML
        );
    }
    
    /**
     * Extrait le nom du fichier depuis le nom de la PropertySource.
     * Ex: "Config resource 'class path resource [application-scenario3.yml]'" -> "application-scenario3.yml"
     */
    private String extractFileName(String sourceName) {
        if (sourceName.contains("[") && sourceName.contains("]")) {
            int start = sourceName.indexOf("[") + 1;
            int end = sourceName.indexOf("]");
            if (end > start) {
                return sourceName.substring(start, end);
            }
        }
        
        // Si on ne peut pas extraire, retourner application.yml
        return "application.yml";
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
