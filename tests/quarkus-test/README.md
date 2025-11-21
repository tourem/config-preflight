# Quarkus Test Project - Config Preflight

This is a test project demonstrating the use of `config-preflight-quarkus`.

## 🚀 Quick Start

### 1. Build and Run

```bash
# Build the project
mvn clean package -DskipTests

# Run with scenario1 (missing properties)
QUARKUS_PROFILE=scenario1 java -jar target/quarkus-app/quarkus-run.jar
```

### 2. Expected Output

You should see a beautiful error report:

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                    ❌  CONFIGURATION VALIDATION FAILED  ❌                     ║
╠══════════════════════════════════════════════════════════════════════════════╣
║                                                                              ║
║   👉 Property: database.password 🔒 [SENSITIVE]                              ║
║      Source:   application.properties                                        ║
║      Error:    Property 'database.password' is not set                       ║
║      💡 Fix:   Add to application.properties: database.password=<value>      ║
║                OR set environment variable: export DATABASE_PASSWORD=<value> ║
║                                                                              ║
║   👉 Property: database.timeout                                              ║
║      Source:   application.properties                                        ║
║      Error:    Property 'database.timeout' is not set                        ║
║      💡 Fix:   Add to application.properties: database.timeout=<value>       ║
║                OR set environment variable: export DATABASE_TIMEOUT=<value>  ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

## 📁 Project Structure

```
src/main/
├── java/
│   └── io/github/tourem/test/quarkus/
│       ├── Application.java
│       └── config/
│           ├── DatabaseConfig.java      (@ConfigMapping)
│           ├── ApiConfig.java           (@ConfigMapping)
│           └── MessagingConfig.java     (@ConfigMapping)
└── resources/
    ├── application.properties           (default config)
    ├── application-scenario1.properties (missing: password, timeout)
    ├── application-scenario2.properties (missing: api properties)
    ├── application-valid.properties     (all properties set)
    └── META-INF/
        └── config-preflight.properties.example  (optional validation config)
```

## ⚙️ Optional Configuration

### Using `config-preflight.properties`

This file is **completely optional**. Config Preflight will work without it by validating all unresolved placeholders.

However, if you want to validate specific `@ConfigMapping` properties, you can create:

**`src/main/resources/META-INF/config-preflight.properties`**

```properties
# Define required properties
required.properties.database.url=true
required.properties.database.password=true
required.properties.api.endpoint=true
```

An example file is provided: `config-preflight.properties.example`

### When to Use It

✅ **Use it when:**
- You want to validate specific `@ConfigMapping` interface properties
- You want explicit control over which properties are required
- You want validation beyond just unresolved placeholders

❌ **Don't use it when:**
- You only use placeholders (`${...}`) - these are validated automatically
- You want zero configuration - just add the dependency

## 🧪 Test Scenarios

### Scenario 1: Missing Database Properties
```bash
QUARKUS_PROFILE=scenario1 java -jar target/quarkus-app/quarkus-run.jar
```
**Expected**: 2 errors (database.password, database.timeout)

### Scenario 2: Missing API Properties
```bash
QUARKUS_PROFILE=scenario2 java -jar target/quarkus-app/quarkus-run.jar
```
**Expected**: 2 errors (api.endpoint, api.cache-directory)

### Scenario 3: Missing Messaging Properties
```bash
QUARKUS_PROFILE=scenario3 java -jar target/quarkus-app/quarkus-run.jar
```
**Expected**: 2 errors (messaging.queue-name, messaging.connection-timeout)

### Valid Scenario: All Properties Set
```bash
QUARKUS_PROFILE=valid java -jar target/quarkus-app/quarkus-run.jar
```
**Expected**: ✅ Application starts successfully

## 🔧 Running Tests

```bash
# Run all test scenarios
./test.sh

# Or manually
mvn clean package -DskipTests
QUARKUS_PROFILE=scenario1 java -jar target/quarkus-app/quarkus-run.jar
```

## 📚 More Information

- [Config Preflight README](../../README.md)
- [Complete Documentation](../../DOCUMENTATION.md)
- [Example config-preflight.properties](src/main/resources/META-INF/config-preflight.properties.example)
