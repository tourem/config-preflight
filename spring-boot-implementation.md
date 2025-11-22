# Implémentation de config-preflight pour Spring Boot

## Contexte du Projet

Je développe une bibliothèque Maven appelée **config-preflight** qui valide TOUTES les propriétés de configuration manquantes au démarrage d'une application Spring Boot, au lieu du comportement par défaut qui affiche seulement la première erreur.

**Problème résolu :**
- Spring Boot avec `@ConfigurationProperties` + `@Validated` affiche une erreur à la fois
- L'utilisateur doit redémarrer plusieurs fois pour corriger toutes les erreurs
- config-preflight affiche TOUTES les erreurs en une seule fois

**Informations du projet :**
- GroupId : `io.github.tourem`
- ArtifactId : `config-preflight-spring-boot`
- Package : `io.github.tourem.configpreflight.springboot`
- Java : 17+
- Spring Boot : 3.x (compatible 2.7+)

## Architecture Existante

Le module `config-preflight-core` existe déjà avec les classes suivantes :
```java
// Classes core déjà disponibles
- io.github.tourem.configpreflight.core.api.ConfigurationValidator
- io.github.tourem.configpreflight.core.api.ValidationResult
- io.github.tourem.configpreflight.core.model.ConfigurationError
- io.github.tourem.configpreflight.core.model.ErrorType
- io.github.tourem.configpreflight.core.model.PropertySource
- io.github.tourem.configpreflight.core.detector.PlaceholderDetector
- io.github.tourem.configpreflight.core.detector.PropertyBindingResolver
- io.github.tourem.configpreflight.core.formatter.BeautifulErrorFormatter
```

## Objectif : Implémentation Hybride

Implémenter une solution hybride qui combine :

1. **FailureAnalyzer** : Intercepte les erreurs de `@ConfigurationProperties` + `@Validated`
    - S'exécute quand Spring Boot échoue au binding
    - Extrait TOUTES les erreurs de validation (pas juste la première)
    - Affiche un message formaté avec toutes les erreurs

2. **ApplicationReadyEvent Listener** : Validation complète après démarrage
    - S'exécute si l'application démarre sans erreur @Validated
    - Valide toutes les propriétés (y compris @Value, placeholders, etc.)
    - Valide le binding automatique (app.database.url ↔ APP_DATABASE_URL)

## Spécifications Techniques

### 1. SpringBootPropertyBindingResolver

**Fichier :** `src/main/java/io/github/tourem/configpreflight/springboot/SpringBootPropertyBindingResolver.java`

**Responsabilités :**
- Vérifier si une propriété existe sous n'importe quelle forme (property, env var, variantes)
- Récupérer la valeur d'une propriété en testant toutes les variantes
- Trouver le nom réel sous lequel une propriété existe
- Générer des suggestions pour corriger les erreurs

**Méthodes requises :**
```java
public class SpringBootPropertyBindingResolver {
    private final Environment environment;
    private final PropertyBindingResolver baseResolver;
    
    public SpringBootPropertyBindingResolver(Environment environment);
    
    // Vérifie si une propriété existe sous n'importe quelle forme
    public boolean propertyExists(String propertyName);
    
    // Récupère la valeur en testant toutes les variantes
    public String getPropertyValue(String propertyName);
    
    // Trouve le nom réel (ex: APP_DATABASE_URL si cherché via app.database.url)
    public String findActualPropertyName(String propertyName);
    
    // Génère un message d'aide
    public String generateSuggestion(String propertyName);
}
```

**Comportement attendu :**
```java
// Exemple 1 : Propriété définie directement
env.setProperty("app.database.url", "value");
resolver.propertyExists("app.database.url") → true

// Exemple 2 : Propriété définie via env var
env.setProperty("APP_DATABASE_URL", "value");
resolver.propertyExists("app.database.url") → true  // ← Binding automatique détecté

// Exemple 3 : Propriété vraiment manquante
resolver.propertyExists("app.missing.property") → false
```

### 2. SpringBootConfigurationValidator

**Fichier :** `src/main/java/io/github/tourem/configpreflight/springboot/SpringBootConfigurationValidator.java`

**Responsabilités :**
- Implémenter l'interface `ConfigurationValidator` du core
- Valider toutes les propriétés de l'Environment
- Détecter les placeholders non résolus
- Utiliser SpringBootPropertyBindingResolver pour les vérifications

**Méthodes requises (de l'interface) :**
```java
@Component
public class SpringBootConfigurationValidator implements ConfigurationValidator {
    private final Environment environment;
    private final PlaceholderDetector placeholderDetector;
    private final SpringBootPropertyBindingResolver bindingResolver;
    
    @Autowired
    public SpringBootConfigurationValidator(Environment environment);
    
    @Override
    public ValidationResult validateAll();
    
    @Override
    public ValidationResult validateRequired(String... requiredProperties);
    
    @Override
    public ValidationResult validatePlaceholders();
}
```

**Détails d'implémentation :**

**validateRequired() :**
- Pour chaque propriété requise :
    - Utiliser `bindingResolver.propertyExists()` pour vérifier TOUTES les variantes
    - Si manquante : ajouter ConfigurationError avec type MISSING_PROPERTY
    - Si présente mais vide : ajouter ConfigurationError avec type EMPTY_VALUE

**validatePlaceholders() :**
- Parcourir toutes les PropertySources de l'Environment
- Pour chaque propriété contenant ${...} :
    - Extraire les placeholders avec PlaceholderDetector
    - Vérifier si chaque placeholder peut être résolu
    - Si non résolu : ajouter ConfigurationError avec type UNRESOLVED_PLACEHOLDER

### 3. ConfigPreflightFailureAnalyzer

**Fichier :** `src/main/java/io/github/tourem/configpreflight/springboot/ConfigPreflightFailureAnalyzer.java`

**Responsabilités :**
- Étendre `AbstractFailureAnalyzer<BindException>`
- Intercepter les erreurs de `@ConfigurationProperties` + `@Validated`
- Extraire TOUTES les erreurs de validation (pas juste la première)
- Formater les erreurs avec BeautifulErrorFormatter

**Signature :**
```java
public class ConfigPreflightFailureAnalyzer extends AbstractFailureAnalyzer<BindException> {
    
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, BindException cause) {
        // Si c'est une BindValidationException
        if (cause.getCause() instanceof BindValidationException) {
            BindValidationException validationException = 
                (BindValidationException) cause.getCause();
            
            // Extraire TOUTES les erreurs
            List<ConfigurationError> errors = extractAllErrors(validationException);
            
            // Formater avec BeautifulErrorFormatter
            String description = new BeautifulErrorFormatter().format(errors);
            
            // Générer les actions
            String action = buildAction(errors);
            
            return new FailureAnalysis(description, action, cause);
        }
        
        return null;
    }
    
    private List<ConfigurationError> extractAllErrors(BindValidationException exception);
    private String buildAction(List<ConfigurationError> errors);
}
```

**Comportement attendu :**
- Extraire tous les ObjectError de la BindValidationException
- Pour chaque FieldError, créer un ConfigurationError
- Utiliser PropertyBindingResolver pour générer les suggestions
- Retourner un FailureAnalysis avec le message formaté

### 4. ConfigPreflightAutoConfiguration

**Fichier :** `src/main/java/io/github/tourem/configpreflight/springboot/ConfigPreflightAutoConfiguration.java`

**Responsabilités :**
- Auto-configuration Spring Boot
- Créer les beans nécessaires
- Enregistrer l'ApplicationListener pour ApplicationReadyEvent
- Permettre de désactiver via propriété

**Code attendu :**
```java
@Configuration
@ConditionalOnProperty(
    name = "config.preflight.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ConfigPreflightAutoConfiguration {
    
    @Bean
    public SpringBootConfigurationValidator configurationValidator(Environment environment) {
        return new SpringBootConfigurationValidator(environment);
    }
    
    @Bean
    public ApplicationListener<ApplicationReadyEvent> configPreflightListener(
            SpringBootConfigurationValidator validator) {
        
        return event -> {
            ValidationResult result = validator.validateAll();
            
            if (!result.isValid()) {
                System.err.println(result.formatErrors());
                throw new ConfigurationValidationException(
                    "Configuration validation failed with " + 
                    result.getErrorCount() + " errors"
                );
            }
        };
    }
}
```

### 5. ConfigurationValidationException

**Fichier :** `src/main/java/io/github/tourem/configpreflight/springboot/ConfigurationValidationException.java`

**Simple RuntimeException :**
```java
public class ConfigurationValidationException extends RuntimeException {
    public ConfigurationValidationException(String message) {
        super(message);
    }
    
    public ConfigurationValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### 6. META-INF/spring.factories

**Fichier :** `src/main/resources/META-INF/spring.factories`

**Contenu :**
```properties
# Auto-configuration
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
io.github.tourem.configpreflight.springboot.ConfigPreflightAutoConfiguration

# Failure Analyzer
org.springframework.boot.diagnostics.FailureAnalyzer=\
io.github.tourem.configpreflight.springboot.ConfigPreflightFailureAnalyzer
```

### 7. pom.xml

**Dépendances requises :**
```xml
<dependencies>
    <!-- Spring Boot (provided) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- Core module -->
    <dependency>
        <groupId>io.github.tourem</groupId>
        <artifactId>config-preflight-core</artifactId>
        <version>${project.version}</version>
    </dependency>
    
    <!-- Bean Validation (provided) -->
    <dependency>
        <groupId>javax.validation</groupId>
        <artifactId>validation-api</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- Tests -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Tests Unitaires

### SpringBootPropertyBindingResolverTest

**Fichier :** `src/test/java/.../SpringBootPropertyBindingResolverTest.java`

**Tests requis :**
```java
@Test
void testPropertyExistsViaDirectProperty() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("app.database.url", "value");
    
    SpringBootPropertyBindingResolver resolver = 
        new SpringBootPropertyBindingResolver(env);
    
    assertTrue(resolver.propertyExists("app.database.url"));
    assertEquals("value", resolver.getPropertyValue("app.database.url"));
}

@Test
void testPropertyExistsViaEnvironmentVariable() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("APP_DATABASE_URL", "value");
    
    SpringBootPropertyBindingResolver resolver = 
        new SpringBootPropertyBindingResolver(env);
    
    // Doit détecter le binding automatique
    assertTrue(resolver.propertyExists("app.database.url"));
    assertEquals("value", resolver.getPropertyValue("app.database.url"));
    assertEquals("APP_DATABASE_URL", resolver.findActualPropertyName("app.database.url"));
}

@Test
void testPropertyDoesNotExist() {
    MockEnvironment env = new MockEnvironment();
    SpringBootPropertyBindingResolver resolver = 
        new SpringBootPropertyBindingResolver(env);
    
    assertFalse(resolver.propertyExists("app.missing.property"));
    assertNull(resolver.getPropertyValue("app.missing.property"));
}

@Test
void testGenerateSuggestion() {
    MockEnvironment env = new MockEnvironment();
    SpringBootPropertyBindingResolver resolver = 
        new SpringBootPropertyBindingResolver(env);
    
    String suggestion = resolver.generateSuggestion("app.database.url");
    
    assertTrue(suggestion.contains("app.database.url"));
    assertTrue(suggestion.contains("APP_DATABASE_URL"));
}
```

### SpringBootConfigurationValidatorTest

**Tests requis :**
```java
@Test
void testValidateRequiredWithMissingProperty() {
    MockEnvironment env = new MockEnvironment();
    SpringBootConfigurationValidator validator = 
        new SpringBootConfigurationValidator(env);
    
    ValidationResult result = validator.validateRequired("app.database.url");
    
    assertFalse(result.isValid());
    assertEquals(1, result.getErrorCount());
    assertEquals(ErrorType.MISSING_PROPERTY, result.getErrors().get(0).getType());
}

@Test
void testValidateRequiredWithPropertyDefinedViaEnvVar() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("APP_DATABASE_URL", "value");
    
    SpringBootConfigurationValidator validator = 
        new SpringBootConfigurationValidator(env);
    
    ValidationResult result = validator.validateRequired("app.database.url");
    
    // Doit être valide car APP_DATABASE_URL définit app.database.url
    assertTrue(result.isValid());
}

@Test
void testValidatePlaceholdersWithUnresolvedPlaceholder() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("app.database.url", "${DATABASE_URL}");
    // DATABASE_URL n'est PAS défini
    
    SpringBootConfigurationValidator validator = 
        new SpringBootConfigurationValidator(env);
    
    ValidationResult result = validator.validatePlaceholders();
    
    assertFalse(result.isValid());
    assertEquals(1, result.getErrorCount());
    assertEquals(ErrorType.UNRESOLVED_PLACEHOLDER, result.getErrors().get(0).getType());
}

@Test
void testValidatePlaceholdersResolvedViaEnvVar() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("app.database.url", "${DATABASE_URL}");
    env.setProperty("DATABASE_URL", "jdbc:postgresql://localhost");
    
    SpringBootConfigurationValidator validator = 
        new SpringBootConfigurationValidator(env);
    
    ValidationResult result = validator.validatePlaceholders();
    
    assertTrue(result.isValid());
}
```

## Critères de Validation

L'implémentation sera considérée comme réussie si :

1. ✅ **Compilation** : Le code compile sans erreurs
2. ✅ **Tests unitaires** : Tous les tests passent au vert
3. ✅ **spring.factories** : Créé au bon endroit avec le bon contenu
4. ✅ **FailureAnalyzer** : Intercepte correctement les BindException
5. ✅ **Binding automatique** : Détecte app.database.url via APP_DATABASE_URL
6. ✅ **Messages formatés** : Utilise BeautifulErrorFormatter
7. ✅ **Auto-configuration** : Fonctionne sans configuration utilisateur
8. ✅ **Désactivable** : Peut être désactivé via config.preflight.enabled=false

## Structure Finale Attendue
```
config-preflight-spring-boot/
├── src/
│   ├── main/
│   │   ├── java/io/github/tourem/configpreflight/springboot/
│   │   │   ├── ConfigPreflightAutoConfiguration.java
│   │   │   ├── ConfigPreflightFailureAnalyzer.java
│   │   │   ├── SpringBootConfigurationValidator.java
│   │   │   ├── SpringBootPropertyBindingResolver.java
│   │   │   └── ConfigurationValidationException.java
│   │   └── resources/
│   │       └── META-INF/
│   │           └── spring.factories
│   └── test/
│       └── java/io/github/tourem/configpreflight/springboot/
│           ├── SpringBootPropertyBindingResolverTest.java
│           └── SpringBootConfigurationValidatorTest.java
└── pom.xml
```

## Notes Importantes

1. **Utilisez les classes du core** : PlaceholderDetector, PropertyBindingResolver, BeautifulErrorFormatter sont déjà implémentés
2. **Spring Boot Environment** : Réutilisez l'Environment de Spring Boot, ne rechargez pas les fichiers
3. **Binding automatique** : Toujours tester TOUTES les variantes (property, env var, etc.)
4. **Messages clairs** : Les erreurs doivent indiquer quelles variantes ont été testées
5. **Non intrusif** : L'utilisateur n'a qu'à ajouter la dépendance, rien d'autre

## Commencez l'implémentation !

Créez tous les fichiers mentionnés ci-dessus en suivant les spécifications.
Commencez par SpringBootPropertyBindingResolver, puis SpringBootConfigurationValidator,
puis ConfigPreflightFailureAnalyzer, et enfin ConfigPreflightAutoConfiguration.

N'oubliez pas spring.factories et les tests unitaires !