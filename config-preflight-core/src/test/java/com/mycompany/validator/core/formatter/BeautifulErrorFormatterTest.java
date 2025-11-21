package com.mycompany.validator.core.formatter;

import com.mycompany.validator.core.api.ValidationResult;
import com.mycompany.validator.core.model.ConfigurationError;
import com.mycompany.validator.core.model.ErrorType;
import com.mycompany.validator.core.model.PropertySource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class BeautifulErrorFormatterTest {
    
    @Test
    public void testFormatWithErrors() {
        BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
        
        // Créer des erreurs de test
        ConfigurationError error1 = ConfigurationError.builder()
                .type(ErrorType.UNRESOLVED_PLACEHOLDER)
                .propertyName("app.database.url")
                .errorMessage("Unresolved placeholder '${DB_URL}'")
                .suggestion("Export env var: export DB_URL=<value>")
                .source(new PropertySource("application.properties", "classpath:/application.properties", PropertySource.SourceType.APPLICATION_PROPERTIES))
                .build();
        
        ConfigurationError error2 = ConfigurationError.builder()
                .type(ErrorType.MISSING_PROPERTY)
                .propertyName("my-service.api-key")
                .errorMessage("Value is required")
                .suggestion("Define property: my-service.api-key=<value>")
                .source(new PropertySource("config/external-config.yml", "file:/config/external-config.yml", PropertySource.SourceType.APPLICATION_YAML))
                .build();
        
        ValidationResult result = new ValidationResult(Arrays.asList(error1, error2));
        
        // Formatter et afficher
        String formatted = formatter.format(result);
        System.out.println(formatted);
    }
    
    @Test
    public void testFormatSuccess() {
        BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();
        ValidationResult result = new ValidationResult(Arrays.asList());
        
        String formatted = formatter.format(result);
        System.out.println(formatted);
    }
}
