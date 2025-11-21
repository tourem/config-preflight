# 📚 Config Preflight - Complete Documentation

## Table of Contents

1. [Context & Motivation](#context--motivation)
2. [The Problem](#the-problem)
3. [The Solution](#the-solution)
4. [Architecture](#architecture)
5. [How It Works](#how-it-works)
6. [Installation](#installation)
7. [Configuration Options](#configuration-options)
8. [Features](#features)
9. [Framework-Specific Details](#framework-specific-details)
10. [Advanced Usage](#advanced-usage)
11. [Testing](#testing)
12. [Troubleshooting](#troubleshooting)

---

## Context & Motivation

### The Development Reality

Modern applications rely heavily on external configuration:
- Database connections
- API keys and secrets
- Service endpoints
- Feature flags
- Environment-specific settings

**The traditional approach:**
1. Application starts
2. Framework initializes beans
3. Bean needs a property → **CRASH**
4. Developer fixes ONE property
5. Restart and repeat

This cycle wastes **hours** of development time and creates frustration.

### The Business Impact

**Development Time**
- Average 15-30 minutes per deployment to discover all configuration issues
- Multiple restarts required
- Slows down CI/CD pipelines

**Security Risks**
- Secrets accidentally logged in error messages
- Sensitive values visible in stacktraces
- Compliance violations

**Developer Experience**
- Frustrating onboarding for new team members
- Difficult to understand what configuration is required
- Cryptic error messages

### Our Goal

Create a **universal validation library** that:
1. ✅ Validates **ALL** configuration at startup
2. ✅ Runs **BEFORE** any bean initialization
3. ✅ Works with **Spring Boot, Quarkus, and Micronaut**
4. ✅ Provides **clear, actionable error messages**
5. ✅ **Protects secrets** from being logged
6. ✅ Requires **zero configuration** to use

---

## The Problem

### Problem 1: Late Detection

Traditional frameworks validate configuration **too late**:

```
Application Startup Flow (Traditional):
1. Load configuration files ✅
2. Initialize ApplicationContext ✅
3. Create DataSource bean ❌ CRASH (missing db.url)
   → 50 lines of stacktrace
   → Application stops
```

**Result**: You only discover ONE error at a time.

### Problem 2: Cryptic Errors

When configuration fails, you get:

```
Error creating bean with name 'dataSource'
  Caused by: java.lang.IllegalArgumentException: jdbcUrl is required
    at com.zaxxer.hikari.HikariConfig.validate(HikariConfig.java:1074)
    at com.zaxxer.hikari.HikariDataSource.getConnection(HikariDataSource.java:112)
    at org.springframework.jdbc.datasource.DataSourceUtils.fetchConnection(DataSourceUtils.java:159)
    [... 47 more lines ...]
```

**Problems:**
- Doesn't tell you which property is missing
- Doesn't suggest how to fix it
- Doesn't show other missing properties

### Problem 3: Security Leaks

Error messages often expose sensitive data:

```
Failed to connect to database
  URL: jdbc:postgresql://prod-db.company.com:5432/mydb
  User: admin
  Password: MyS3cr3tP@ssw0rd  ← 🚨 LEAKED!
```

### Problem 4: Multiple Restarts

Typical scenario:
1. Start app → Missing `db.url` → Fix → Restart
2. Start app → Missing `db.password` → Fix → Restart
3. Start app → Missing `redis.host` → Fix → Restart
4. Start app → Missing `api.key` → Fix → Restart
5. Finally works! ✅

**Time wasted**: 15-30 minutes

---

## The Solution

### Our Approach

**Config Preflight** solves all these problems by:

1. **Early Validation**: Runs BEFORE any bean initialization
2. **Complete Scanning**: Detects ALL errors at once
3. **Smart Detection**: Understands placeholders and defaults
4. **Security First**: Automatically masks secrets
5. **Beautiful Reporting**: Clear, actionable error messages

### The Result

```
Application Startup Flow (With Validator):
1. Load configuration files ✅
2. Config Preflight runs ✅
   → Scans ALL properties
   → Detects ALL errors
   → Shows beautiful report
   → Blocks startup if errors found
3. (Beans never initialize if config is invalid)
```

**Result**: Fix all errors at once, restart once, done!

---

## Architecture

### Module Structure

```
config-preflight/
├── config-preflight-core/          # Framework-agnostic logic
│   ├── api/                               # Public interfaces
│   ├── model/                             # Error models
│   ├── detector/                          # Placeholder & secret detection
│   └── formatter/                         # Error formatting
│
├── config-preflight-spring-boot/   # Spring Boot integration
│   ├── SpringBootValidatorEnvironmentPostProcessor
│   ├── SpringBootBinderPropertyResolver
│   └── ConfigurationValidationFailureAnalyzer
│
├── config-preflight-quarkus/       # Quarkus integration
│   ├── QuarkusEarlyValidator
│   └── QuarkusConfigurationValidator
│
└── config-preflight-micronaut/     # Micronaut integration
    ├── MicronautEarlyValidator
    └── MicronautConfigurationValidator
```

### Core Components

#### 1. PlaceholderDetector
Detects `${...}` placeholders in property values.

**Key Features:**
- Detects all placeholders: `${property}`
- Detects placeholders with defaults: `${property:default}`
- **Smart Rule**: Ignores placeholders with defaults (they're optional)

```java
// ❌ Error: No default value
app.url=${APP_URL}

// ✅ OK: Has default value
app.url=${APP_URL:http://localhost:8080}
```

#### 2. SecretDetector
Identifies sensitive properties and masks their values.

**Detected Keywords:**
- password, passwd, pwd
- secret, token, key, apikey
- credential, auth, authorization
- private, certificate, cert

**Masking:**
```java
// Before
Property 'api.secret' has value 'my-secret-123'

// After
Property 'api.secret' 🔒 [SENSITIVE] has value ***MASKED***
```

#### 3. BeautifulErrorFormatter
Formats errors in a clear, actionable way.

**Features:**
- ASCII box design with double borders
- Emoji icons for error types
- Grouped by error type
- Actionable suggestions
- Secret masking

#### 4. Framework Resolvers
Each framework has a custom resolver that understands its binding rules:

- **SpringBootBinderPropertyResolver**: Uses Spring's `Binder` API
- **QuarkusPropertyBindingResolver**: Uses MicroProfile Config
- **MicronautPropertyBindingResolver**: Uses Micronaut Environment

---

## How It Works

### Spring Boot

#### 1. EnvironmentPostProcessor
```java
@Override
public void postProcessEnvironment(ConfigurableEnvironment environment, 
                                   SpringApplication application) {
    // Runs AFTER application.properties is loaded
    // Runs BEFORE any bean is created
    
    List<ConfigurationError> errors = validatePlaceholders(environment);
    
    if (!errors.isEmpty()) {
        throw new ConfigurationValidationException(errors);
    }
}
```

**Registered in**: `META-INF/spring.factories`
```properties
org.springframework.boot.env.EnvironmentPostProcessor=\
io.github.tourem.springboot.SpringBootValidatorEnvironmentPostProcessor
```

#### 2. Binder API
Uses Spring Boot's internal `Binder` for 100% faithful property resolution:

```java
Binder binder = Binder.get(environment);
BindResult<String> result = binder.bind("app.database.url", String.class);

if (result.isBound()) {
    // Property exists (handles relaxed binding automatically)
}
```

**Relaxed Binding Examples:**
- `app.database.url` → Found as `APP_DATABASE_URL`
- `my-service.api-key` → Found as `MY_SERVICE_API_KEY`
- `server.port` → Found as `SERVER_PORT`

#### 3. FailureAnalyzer
Catches `ConfigurationValidationException` and formats it beautifully:

```java
@Override
protected FailureAnalysis analyze(Throwable rootFailure, 
                                  ConfigurationValidationException cause) {
    String description = formatter.format(cause.getValidationResult());
    String action = buildActionMessage(cause.getValidationResult());
    
    return new FailureAnalysis(description, action, cause);
}
```

**Registered in**: `META-INF/spring.factories`
```properties
org.springframework.boot.diagnostics.FailureAnalyzer=\
io.github.tourem.springboot.ConfigurationValidationFailureAnalyzer
```

### Quarkus

#### 1. Startup Observer
```java
@ApplicationScoped
public class QuarkusEarlyValidator {
    
    public void onStart(@Observes @Priority(25) StartupEvent event) {
        // Priority 25 = PLATFORM_BEFORE
        // Runs BEFORE infrastructure beans
        
        Config config = ConfigProvider.getConfig();
        List<ConfigurationError> errors = validatePlaceholders(config);
        
        if (!errors.isEmpty()) {
            throw new ConfigurationValidationException(errors);
        }
    }
}
```

**Priority Levels:**
- `25` = PLATFORM_BEFORE (our validator)
- `50` = PLATFORM (infrastructure beans)
- `100` = APPLICATION (user beans)

#### 2. MicroProfile Config
Uses standard MicroProfile Config API:

```java
Config config = ConfigProvider.getConfig();
Optional<String> value = config.getOptionalValue("app.url", String.class);
```

### Micronaut

#### 1. Event Listener
```java
@Singleton
public class MicronautEarlyValidator 
        implements ApplicationEventListener<StartupEvent>, Ordered {
    
    @Override
    public void onApplicationEvent(StartupEvent event) {
        // Runs at startup
        
        List<ConfigurationError> errors = validatePlaceholders();
        
        if (!errors.isEmpty()) {
            throw new ConfigurationValidationException(errors);
        }
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE; // Run first
    }
}
```

#### 2. Environment API
Uses Micronaut's Environment:

```java
Optional<String> value = environment.getProperty("app.url", String.class);
```

---

## Installation

### Prerequisites
- JDK 17 or higher
- Maven 3.6+ or Gradle 7+
- Spring Boot 3.x, Quarkus 3.x, or Micronaut 4.x

### Maven Installation

#### 1. Add to your `pom.xml`

**Spring Boot:**
```xml
<dependency>
    <groupId>io.github.tourem</groupId>
    <artifactId>config-preflight-spring-boot</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Quarkus:**
```xml
<dependency>
    <groupId>io.github.tourem</groupId>
    <artifactId>config-preflight-quarkus</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Micronaut:**
```xml
<dependency>
    <groupId>io.github.tourem</groupId>
    <artifactId>config-preflight-micronaut</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### 2. Build the Library (First Time Only)

```bash
cd config-preflight
mvn clean install
```

#### 3. That's It!

The validator activates automatically:

- **Spring Boot** ✅ Automatically scans `@ConfigurationProperties` beans
- **Micronaut** ✅ Automatically scans `@ConfigurationProperties` beans  
- **Quarkus** ⚠️ Automatically validates placeholders + optional custom validation

### Optional: Custom Property Validation (Quarkus Only)

For Quarkus projects, you can optionally define specific properties to validate by creating:

**`src/main/resources/META-INF/config-preflight.properties`**

```properties
# Define required properties to validate
required.properties.database.url=true
required.properties.database.username=true
required.properties.database.password=true
required.properties.database.max-connections=true

required.properties.api.endpoint=true
required.properties.api.api-key=true
required.properties.api.retry-count=true

required.properties.messaging.broker-url=true
required.properties.messaging.queue-name=true
```

**When to use this:**
- ✅ You want to validate specific `@ConfigMapping` interface properties
- ✅ You want explicit control over which properties are required
- ✅ You want validation beyond just unresolved placeholders

**When NOT needed:**
- ❌ You only use placeholders (`${...}`) - these are validated automatically
- ❌ You're using Spring Boot or Micronaut - they scan automatically

> **Note**: This file is **completely optional**. Without it, Quarkus will still validate all unresolved placeholders in your configuration.

---

## Configuration Options

### Available Properties

```properties
# Enable/disable the validator entirely
# Default: true
configuration.validator.enabled=true

# Enable/disable early validation
# If false, validation runs as a normal bean (later in lifecycle)
# Default: true
configuration.validator.early-validation=true
```

### Disabling for Tests

Create `application-test.properties`:

```properties
# Disable validator in tests
configuration.validator.enabled=false
```

Run tests with:
```bash
mvn test -Dspring.profiles.active=test
```

### Disabling for Specific Profiles

```properties
# application-dev.properties
configuration.validator.enabled=true

# application-prod.properties
configuration.validator.enabled=true

# application-local.properties
configuration.validator.enabled=false  # More flexible for local dev
```

---

## Features

### 1. Placeholder Detection

#### Basic Placeholders
```yaml
app:
  url: ${APP_URL}  # ❌ Required
```

#### Placeholders with Defaults
```yaml
app:
  host: ${APP_HOST:localhost}  # ✅ Optional (has default)
  port: ${APP_PORT:8080}       # ✅ Optional (has default)
```

#### Nested Placeholders
```yaml
app:
  url: ${APP_PROTOCOL:http}://${APP_HOST}:${APP_PORT:8080}
  # APP_HOST is required
  # APP_PROTOCOL and APP_PORT are optional
```

### 2. Relaxed Binding

The validator understands all binding variants:

| Property File | Environment Variable | Matches |
|---------------|---------------------|---------|
| `app.database.url` | `APP_DATABASE_URL` | ✅ |
| `app.database-url` | `APP_DATABASE_URL` | ✅ |
| `app.databaseUrl` | `APP_DATABASE_URL` | ✅ |
| `my-service.api-key` | `MY_SERVICE_API_KEY` | ✅ |

### 3. Secret Detection

Automatically detects and masks sensitive properties:

**Detected Patterns:**
- `*.password`, `*.passwd`, `*.pwd`
- `*.secret`, `*.token`, `*.key`
- `*.apikey`, `*.api-key`, `*.api_key`
- `*.credential`, `*.auth`, `*.authorization`
- `*.private`, `*.certificate`, `*.cert`

**Example:**
```yaml
database:
  url: ${DB_URL}           # Normal property
  password: ${DB_PASSWORD} # 🔒 Sensitive property
```

**Output:**
```
● Property: database.password 🔒 [SENSITIVE]
  ❌ Cannot resolve placeholder '${DB_PASSWORD}'
  💡 export DB_PASSWORD=<value>
  
  Note: Value is masked for security
```

### 4. Error Grouping

Errors are grouped by type for clarity:

```
📊 Summary:
   Total Errors: 5

   🔴 Missing Properties: 2
   🔶 Unresolved Placeholders: 3

───────────────────────────────────────────────────────────────────────────────
  🔴  Missing Properties
───────────────────────────────────────────────────────────────────────────────

  ● Property: app.name
    ❌ Property is required but not defined
    💡 Define property: app.name=<value>

───────────────────────────────────────────────────────────────────────────────
  🔶  Unresolved Placeholders
───────────────────────────────────────────────────────────────────────────────

  ● Property: app.database.url
    ❌ Cannot resolve placeholder '${DATABASE_URL}'
    💡 export DATABASE_URL=<value>
```

### 5. Actionable Suggestions

Every error includes a suggestion:

```
● Property: app.database.url
  ❌ Cannot resolve placeholder '${DATABASE_URL}'
  💡 export DATABASE_URL=<value>
     Or add to application.properties:
     app.database.url=jdbc:postgresql://localhost:5432/mydb
```

---

## Framework-Specific Details

### Spring Boot

**Validation Timing:**
```
1. Load application.properties/yml ✅
2. EnvironmentPostProcessor runs ← VALIDATION HERE
3. Create ApplicationContext
4. Initialize Beans (DataSource, JPA, etc.)
```

**Integration Points:**
- `EnvironmentPostProcessor`: Early validation
- `Binder API`: Property resolution
- `FailureAnalyzer`: Error formatting

**Configuration:**
```properties
# Spring Boot specific
configuration.validator.enabled=true
configuration.validator.early-validation=true
```

### Quarkus

**Validation Timing:**
```
1. Load application.properties
2. StartupEvent fired ← VALIDATION HERE (Priority 25)
3. Initialize CDI beans
4. Start services
```

**Integration Points:**
- `@Observer(StartupEvent)`: Early validation
- `ConfigProvider`: Property resolution
- `@Priority(25)`: Run before infrastructure

**Configuration:**
```properties
# Quarkus specific
configuration.validator.enabled=true
configuration.validator.early-validation=true
```

### Micronaut

**Validation Timing:**
```
1. Load application.yml
2. StartupEvent fired ← VALIDATION HERE (HIGHEST_PRECEDENCE)
3. Initialize beans
4. Start services
```

**Integration Points:**
- `ApplicationEventListener<StartupEvent>`: Early validation
- `Environment`: Property resolution
- `Ordered.HIGHEST_PRECEDENCE`: Run first

**Configuration:**
```properties
# Micronaut specific
configuration.validator.enabled=true
configuration.validator.early-validation=true
```

---

## Advanced Usage

### Custom Validation

You can inject the validator and use it programmatically:

#### Spring Boot
```java
@Component
public class MyCustomValidator {
    
    @Autowired
    private SpringBootConfigurationValidator validator;
    
    public void validateMyProperties() {
        ValidationResult result = validator.validateRequired(
            "my.custom.property1",
            "my.custom.property2"
        );
        
        if (result.hasErrors()) {
            // Handle errors
        }
    }
}
```

#### Quarkus
```java
@ApplicationScoped
public class MyCustomValidator {
    
    @Inject
    QuarkusConfigurationValidator validator;
    
    public void validateMyProperties() {
        ValidationResult result = validator.validateRequired(
            "my.custom.property1",
            "my.custom.property2"
        );
        
        if (result.hasErrors()) {
            // Handle errors
        }
    }
}
```

### Custom Secret Keywords

Add custom keywords for secret detection:

```java
SecretDetector detector = new SecretDetector();
detector.addSensitiveKeywords("internal-key", "company-secret");
```

### Custom Error Formatting

```java
BeautifulErrorFormatter formatter = new BeautifulErrorFormatter();

// Full format
String fullReport = formatter.format(validationResult);

// Compact format
String compactReport = formatter.formatCompact(validationResult);
```

---

## Testing

### Unit Tests

The library includes comprehensive unit tests:

```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl config-preflight-core
mvn test -pl config-preflight-spring-boot
```

### Integration Tests

Create integration tests for your application:

```java
@SpringBootTest
class ConfigurationValidationIntegrationTest {
    
    @Test
    void shouldFailWithMissingProperties() {
        // Remove required properties
        System.clearProperty("DATABASE_URL");
        
        // Application should fail to start
        assertThrows(ConfigurationValidationException.class, () -> {
            SpringApplication.run(MyApplication.class);
        });
    }
    
    @Test
    void shouldStartWithAllProperties() {
        // Set all required properties
        System.setProperty("DATABASE_URL", "jdbc:h2:mem:test");
        System.setProperty("API_KEY", "test-key");
        
        // Application should start successfully
        assertDoesNotThrow(() -> {
            SpringApplication.run(MyApplication.class);
        });
    }
}
```

---

## Troubleshooting

### Validator Not Running

**Problem**: Validator doesn't seem to run.

**Solutions:**
1. Check if it's enabled:
   ```properties
   configuration.validator.enabled=true
   ```

2. Check if early validation is enabled:
   ```properties
   configuration.validator.early-validation=true
   ```

3. Verify the dependency is in your `pom.xml`

4. Check Spring Boot version (must be 3.x)

### False Positives

**Problem**: Validator reports errors for properties that exist.

**Solutions:**
1. Check property name spelling
2. Verify environment variable format (use uppercase with underscores)
3. Check if property has a default value

### Secrets Not Masked

**Problem**: Sensitive values appear in logs.

**Solutions:**
1. Check property name contains a sensitive keyword
2. Add custom keywords if needed:
   ```java
   detector.addSensitiveKeywords("my-secret-property");
   ```

### Performance Impact

**Problem**: Startup time increased.

**Analysis:**
- Validation typically adds 50-200ms to startup
- This is negligible compared to bean initialization time
- The time saved by avoiding restarts far outweighs this cost

**If needed:**
```properties
# Disable for local development
configuration.validator.enabled=false
```

---

## Performance

### Benchmarks

Typical validation times:

| Properties | Time |
|-----------|------|
| 10 | ~10ms |
| 50 | ~50ms |
| 100 | ~100ms |
| 500 | ~200ms |

**Note**: This is a one-time cost at startup, negligible compared to:
- Bean initialization: 1-5 seconds
- Database connection: 500ms-2s
- Application startup: 5-30 seconds

### Memory Usage

- Core library: ~2MB
- Runtime overhead: ~100KB

---

## Best Practices

### 1. Use Defaults for Optional Properties
```yaml
# Good
app:
  host: ${APP_HOST:localhost}
  port: ${APP_PORT:8080}

# Bad (requires env vars even for local dev)
app:
  host: ${APP_HOST}
  port: ${APP_PORT}
```

### 2. Group Related Properties
```yaml
# Good
database:
  url: ${DB_URL}
  username: ${DB_USERNAME}
  password: ${DB_PASSWORD}

# Less clear
db-url: ${DB_URL}
db-user: ${DB_USERNAME}
db-pass: ${DB_PASSWORD}
```

### 3. Use Descriptive Property Names
```yaml
# Good
app.external-api.timeout-seconds: ${API_TIMEOUT:30}

# Bad
app.timeout: ${TIMEOUT:30}
```

### 4. Document Required Properties
Create a `.env.example` file:
```bash
# Required
DATABASE_URL=jdbc:postgresql://localhost:5432/mydb
DATABASE_PASSWORD=changeme
API_KEY=your-api-key-here

# Optional (have defaults)
APP_HOST=localhost
APP_PORT=8080
```

---

## FAQ

### Q: Does this work with Spring Cloud Config?
**A:** Yes! The validator runs after all property sources are loaded, including Spring Cloud Config.

### Q: Can I use this with Docker?
**A:** Yes! Pass environment variables via Docker:
```bash
docker run -e DATABASE_URL=... -e API_KEY=... myapp
```

### Q: Does this work with Kubernetes ConfigMaps?
**A:** Yes! ConfigMaps are loaded as environment variables or property files, both supported.

### Q: Can I disable validation for specific profiles?
**A:** Yes! Use profile-specific properties:
```properties
# application-test.properties
configuration.validator.enabled=false
```

### Q: What about Spring Boot 2.x?
**A:** This library targets Spring Boot 3.x. For 2.x, you would need to adjust dependencies.

---

## Roadmap

### Version 1.1 (Planned)
- [ ] Support for SpEL expressions
- [ ] Type validation (int, boolean, etc.)
- [ ] JSON export of errors
- [ ] Metrics integration

### Version 2.0 (Future)
- [ ] Kotlin support
- [ ] Async validation
- [ ] Maven/Gradle plugins
- [ ] GraalVM Native Image support

---

## Support

### Documentation
- [README.md](README.md) - Quick start guide
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines

### Issues
Report issues on GitHub: [Issues](https://github.com/yourorg/config-preflight/issues)

### Community
- Stack Overflow: Tag `config-preflight`
- Discussions: [GitHub Discussions](https://github.com/yourorg/config-preflight/discussions)

---

## License

Apache License 2.0 - See [LICENSE](LICENSE) for details.

---

**Built with ❤️ for developers who are tired of configuration headaches.**
